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
package com.crossbusiness.resiliency.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.crossbusiness.resiliency.annotation.Async;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract aspect that routes selected methods asynchronously.
 *
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 *
 */
@Aspect
public abstract class AbstractAsyncAspect {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<Method, AsyncTaskExecutor>(16);
	
	@Autowired
	private ApplicationContext context;

    private Executor defaultExecutor;

    @Autowired(required=false)
    @Qualifier("defaultExecutor")
    public void setExecutor(Executor defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Around("asyncAnnotatedClass() && @target(asyncConfig)")
	public Object asyncCallOnClassLevel(ProceedingJoinPoint pjp, Async asyncConfig) throws Throwable {
		return doAsync(pjp, asyncConfig);
	}
    @Around("asyncAnnotatedMethod() && @annotation(asyncConfig)")
	public Object asyncCallOnMethodLevel(ProceedingJoinPoint pjp, Async asyncConfig) throws Throwable {
		return doAsync(pjp, asyncConfig);
	}
    
	/**
	 * Intercept the given method invocation, submit the actual calling of the method to
	 * the correct task executor and return immediately to the caller.
	 * @return {@link Future} if the original method returns {@code Future}; {@code null}
	 * otherwise.
	 */
    // @Around("asyncMethodExecution()")
	private Object doAsync(final ProceedingJoinPoint point, Async asyncConfig) throws Throwable {
        log.debug(point + " -> " + asyncConfig);

        String qualifier = asyncConfig.value();
		MethodSignature targetMethodSig = (MethodSignature) point.getSignature();
        AsyncTaskExecutor executor = getExecutor(targetMethodSig.getMethod(), qualifier);

        if (executor == null) {
            return point.proceed();
        }

        Callable<Object> callable = new Callable<Object>() {
            public Object call() throws Exception {
                try {
                    Object result = point.proceed();
                    if (result instanceof Future) {
                        return ((Future<?>) result).get();
                    }
                } catch (Throwable ex) {
                    ReflectionUtils.rethrowException(ex);
                }
                return null;
            }
        };

        Future<?> result = executor.submit(callable);

		if (Future.class.isAssignableFrom(targetMethodSig.getMethod().getReturnType())) {
			return result;
		}
		else {
			return null;
		}
	}
	
	protected AsyncTaskExecutor getExecutor(Method method, String qualifier) {
        AsyncTaskExecutor executor = this.executors.get(method);
		if (executor == null) {
            Executor executorToUse = this.defaultExecutor;
            if (qualifier != null && !qualifier.isEmpty()) {
                try {
                    executorToUse = context.getBean(qualifier, Executor.class);
                } catch(NoSuchBeanDefinitionException ex){
                    //NoSuchBeanDefinitionException | NoUniqueBeanDefinitionException
                    log.error("Executor with qualifier: "+qualifier+" Not defined in spring context");
                }
            }
            if (executorToUse == null) {
                return null;
            }
            executor = (executorToUse instanceof AsyncTaskExecutor ?
                    (AsyncTaskExecutor) executorToUse : new TaskExecutorAdapter(executorToUse));
            this.executors.put(method, executor);
		}
		return executor;
	}

    @Pointcut
    public abstract void asyncAnnotatedMethod();

    @Pointcut
    public abstract void asyncAnnotatedClass();

    @Pointcut
    public abstract void asyncMethodExecution();
}
