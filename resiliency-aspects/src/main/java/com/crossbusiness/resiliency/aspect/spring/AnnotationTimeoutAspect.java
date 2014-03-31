/**
 * The MIT License (MIT)
 *
 * Copyright (c)  2014 CrossBusiness, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.crossbusiness.resiliency.aspect.spring;

import com.crossbusiness.resiliency.annotation.Timeout;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareError;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/12/14.
 */
@Component
@Order(115)
@Aspect
public class AnnotationTimeoutAspect {
    static final Logger log = LoggerFactory.getLogger(AnnotationTimeoutAspect.class);

    /**
     * Calls being watched.
     */
    private final transient Set<AnnotationTimeoutAspect.Call> calls =
            new ConcurrentSkipListSet<AnnotationTimeoutAspect.Call>();

    /**
     * Service that interrupts threads.
     */
    private final transient ScheduledExecutorService interrupter =
            Executors.newSingleThreadScheduledExecutor(
                    threadFactory()
            );

    private ThreadFactory threadFactory() {
        CustomizableThreadFactory tf =  new CustomizableThreadFactory("sumo-timeout-");
        tf.setThreadPriority(Thread.MAX_PRIORITY);
        tf.setDaemon(true);
        tf.setThreadGroupName("resiliency");
        return tf;
    }

    /**
     * Public ctor.
     */
    public AnnotationTimeoutAspect() {
        this.interrupter.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        AnnotationTimeoutAspect.this.interrupt();
                    }
                },
                1, 1, TimeUnit.SECONDS //Time resolution is one second
        );
    }

    @Around("timeoutAnnotatedMethod(timeoutConfig)")
    public Object timeoutOnMethodLevel(final ProceedingJoinPoint point,Timeout timeoutConfig) throws Throwable {
        return  doTimeout(point,timeoutConfig);
    }

    @Around("timeoutAnnotatedClass(timeoutConfig)")
    public Object timeoutOnClassLevel(final ProceedingJoinPoint point,Timeout timeoutConfig) throws Throwable {
        return  doTimeout(point,timeoutConfig);
    }

    // @Around("timeoutMethodExecution(timeoutConfig)")
    public Object doTimeout(final ProceedingJoinPoint point,Timeout timeoutConfig) throws Throwable {
        log.debug(point + " -> " + timeoutConfig);
        final AnnotationTimeoutAspect.Call call = new AnnotationTimeoutAspect.Call(point, timeoutConfig);
        this.calls.add(call);
        Object output;
        try {
            output = point.proceed();
        } finally {
            this.calls.remove(call);
        }
        return output;
    }

    /**
     * Interrupt threads when needed.
     */
    private void interrupt() {
        synchronized (this.interrupter) {
            for (AnnotationTimeoutAspect.Call call : this.calls) {
                if (call.expired() && call.interrupted()) {
                    this.calls.remove(call);
                }
            }
        }
    }

    /**
     * A call being watched.
     */
    private static final class Call implements
            Comparable<AnnotationTimeoutAspect.Call> {
        /**
         * The thread called.
         */
        private final transient Thread thread = Thread.currentThread();
        /**
         * When started.
         */
        private final transient long start = System.currentTimeMillis();
        /**
         * When will expire.
         */
        private final transient long deadline;
        /**
         * Join point.
         */
        private final transient ProceedingJoinPoint point;
        /**
         * Public ctor.
         * @param pnt Joint point
         */
        public Call(final ProceedingJoinPoint pnt, Timeout timeoutConfig) {
            this.point = pnt;
            this.deadline = this.start + timeoutConfig.unit().toMillis(timeoutConfig.value());
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Call obj) {
            int compare;
            if (this.deadline > obj.deadline) {
                compare = 1;
            } else if (this.deadline < obj.deadline) {
                compare = -1;
            } else {
                compare = 0;
            }
            return compare;
        }
        /**
         * Is it expired already?
         * @return TRUE if expired
         */
        public boolean expired() {
            return this.deadline < System.currentTimeMillis();
        }
        /**
         * This thread is stopped already (interrupt if not)?
         * @return TRUE if it's already dead
         */
        public boolean interrupted() {
            boolean dead;
            if (this.thread.isAlive()) {
                this.thread.interrupt();
                final Method method = MethodSignature.class
                        .cast(this.point.getSignature())
                        .getMethod();
                log.warn(
                        "{}: interrupted on {}ms timeout (over {}ms)",
                        new Object[] {method,
                                System.currentTimeMillis() - this.start,
                                this.deadline - this.start}
                );
                dead = false;
            } else {
                dead = true;
            }
            return dead;
        }
    }

    /**
     * Matches the execution of any method with the @{@link com.crossbusiness.resiliency.annotation.Timeout} annotation.
     */
    @Pointcut("execution(@com.crossbusiness.resiliency.annotation.Timeout * *(..)) && @annotation(timeoutConfig)")
    public void timeoutAnnotatedMethod(Timeout timeoutConfig) {}

    /**
     * Matches the execution of any public method in a type with the @{@link com.crossbusiness.resiliency.annotation.Timeout}
     * annotation, or any subtype of a type with the {@code Timeout} annotation.
     */
    @Pointcut("execution(public * ((@com.crossbusiness.resiliency.annotation.Timeout *)+).*(..)) " +
            "&& within(@com.crossbusiness.resiliency.annotation.Timeout *) && @target(timeoutConfig) " +
            "&& !com.crossbusiness.resiliency.aspect.SystemArchitecture.groovyMOPMethods()")
    public void timeoutAnnotatedClass(Timeout timeoutConfig) {}

    /**
     * Definition of pointcut from super aspect - matched join points
     * will have Timeout Aspect applied.
     */
    @Pointcut("(timeoutAnnotatedMethod(timeoutConfig) || timeoutAnnotatedClass(timeoutConfig))")
    public void timeoutMethodExecution(Timeout timeoutConfig) { }

    @DeclareError("execution(@com.crossbusiness.resiliency.annotation.Timeout  * *(..) throws !java.lang.InterruptedException)")
    static final String anError = "Only methods that are declared with throws InterruptedException may have an @Timeout annotation";
}
