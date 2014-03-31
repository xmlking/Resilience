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

import com.crossbusiness.resiliency.annotation.Timeout2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareError;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

/**
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/12/14.
 */
@Component
@Order(116)
@Aspect
public class AnnotationTimeout2Aspect {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static class TimeoutThread extends Thread {
        private boolean completed = false;
        private ProceedingJoinPoint point;
        private Throwable throwable;
        private Object value;

        public ProceedingJoinPoint getPoint() {
            return point;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public Object getValue() {
            return value;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void run() {
            try {
                setValue(point.proceed());
            } catch (Throwable t) {
                setThrowable(t);
            } finally {
                setCompleted(true);
            }
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public void setPoint(ProceedingJoinPoint point) {
            this.point = point;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        public void setValue(Object value) {
            this.value = value;
        }

    }


    @Around("timeoutAnnotatedClass(timeoutConfig)")
    public Object timeoutOnClassLevel(final ProceedingJoinPoint point,Timeout2 timeoutConfig) throws Throwable {
        return  doTimeout(point,timeoutConfig);
    }

    @Around("timeoutAnnotatedMethod(timeoutConfig)")
    public Object timeoutOnMethodLevel(final ProceedingJoinPoint point,Timeout2 timeoutConfig) throws Throwable {
        return  doTimeout(point,timeoutConfig);
    }

    // @Around("timeoutMethodExecution(timeoutConfig)")
    public Object doTimeout(final ProceedingJoinPoint point,Timeout2 timeoutConfig) throws Throwable {
        log.debug(point + " -> " + timeoutConfig);
        Method method = ((MethodSignature) point.getSignature()).getMethod();

        TimeoutThread thread = new TimeoutThread();
        thread.setDaemon(timeoutConfig.daemon());
        thread.setPoint(point);
        thread.start();
        thread.join(timeoutConfig.unit().toMillis(timeoutConfig.value()));

        if (!thread.isCompleted()) {
            throw new TimeoutException("Method " + method + " exceeded timeout of " + timeoutConfig.unit().toMillis(timeoutConfig.value()) + " ms");
        } else if (thread.getThrowable() != null) {
            throw thread.getThrowable();
        } else {
            return thread.getValue();
        }
    }
    @Pointcut("execution(@com.crossbusiness.resiliency.annotation.Timeout2 * *(..)) && @annotation(timeoutConfig)")
    public void timeoutAnnotatedMethod(Timeout2 timeoutConfig) {}

    @Pointcut("execution(public * ((@com.crossbusiness.resiliency.annotation.Timeout2 *)+).*(..)) " +
            "&& within(@com.crossbusiness.resiliency.annotation.Timeout2 *) && @target(timeoutConfig) " +
            "&& !com.crossbusiness.resiliency.aspect.SystemArchitecture.groovyMOPMethods()")
    public void timeoutAnnotatedClass(Timeout2 timeoutConfig) {}

    @Pointcut("(timeoutAnnotatedMethod(timeoutConfig) || timeoutAnnotatedClass(timeoutConfig))")
    public void timeoutMethodExecution(Timeout2 timeoutConfig) { }

    @DeclareError("execution(@com.crossbusiness.resiliency.annotation.Timeout2  * *(..) throws !java.util.concurrent.TimeoutException)")
    static final String anError = "Only methods that are declared with throws TimeoutException may have an @Timeout2 annotation";
}
