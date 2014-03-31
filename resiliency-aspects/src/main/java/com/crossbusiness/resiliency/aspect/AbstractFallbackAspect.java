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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.crossbusiness.resiliency.annotation.Fallback;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 *
 */
@Aspect
public abstract class AbstractFallbackAspect {
    protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ApplicationContext context;

    @Around("fallbackAnnotatedClass(fallbackConfig)")
	public Object fallbackOnClassLevel(ProceedingJoinPoint pjp, Fallback fallbackConfig) throws Throwable {
		return rerouteToFallback(pjp, fallbackConfig);
	}

//    @AfterThrowing(
//            pointcut="@annotation(fallbackConfig)",
//            throwing="ex")
    @Around("fallbackAnnotatedMethod(fallbackConfig)")
	public Object fallbackOnMethodLevel(ProceedingJoinPoint pjp, Fallback fallbackConfig) throws Throwable {
		return rerouteToFallback(pjp, fallbackConfig);
	}

    // @Around("fallbackMethodExecution(fallbackConfig)")
	public Object rerouteToFallback(ProceedingJoinPoint pjp, Fallback fallbackConfig) throws Throwable {
		
		String[] fallbacks = fallbackConfig.value();
		Class<? extends Throwable>[] fallbackableExceptions = fallbackConfig.exceptions();
		
		List<Object> fallbackBeans = new ArrayList<Object>(fallbacks.length);
		for (String fallback : fallbacks) {
			try {
				fallbackBeans.add(context.getBean(fallback));
			}catch (BeansException   be) {
				log.error("configuration error: cannot find bean with name: '{}'",fallback, be); 
				//configuration errors should be fixed immediately.    
				throw be;
			}
		}
			
		MethodSignature targetMethodSig = (MethodSignature) pjp.getSignature();
		Method targetMethod = targetMethodSig.getMethod();
		Class[] paramTypes = (Class[]) targetMethod.getParameterTypes();
		Object[] args = pjp.getArgs();
		
		log.debug("fallbacks: {} method: '{}'",fallbacks, targetMethod);
		
		try {
			return pjp.proceed();
		}catch (Throwable t) {
			
            // if the exception is not what we're looking for, rethrow it
        	if (!isFallbackableException(t, fallbackableExceptions)) throw t;
        	
			log.debug("got exception while trying the targetBean method: '{}'. will try fallbackBean...",targetMethod);
			Iterator<Object> iter = fallbackBeans.iterator();
	        while (iter.hasNext()) {
	        	Object fallbackBean = iter.next();	
				Method fallbackMethod;
				try {
					fallbackMethod = fallbackBean.getClass().getMethod(targetMethod.getName(),paramTypes);
				}catch(NoSuchMethodException | SecurityException  nsme) {
					log.error("configuration error: No matchig method found in fallbackBean: '{}' that matches to targetBean method: '{}'", new Object[] {fallbackBean.getClass().getName(), targetMethod, nsme}); 
					//configuration errors should be fixed immediately.   
				    throw nsme;
				}
	            try{	
					log.debug("trying fallbackBean method: '{}'...",fallbackMethod);
	    			return  fallbackMethod.invoke(fallbackBean, args);
	            } catch(IllegalArgumentException | IllegalAccessException iae) {
	            	log.error("configuration error: arguments missmatch: fallbackBean method: '{}' arguments  missmatch to targetBean method: '{}' arguments", new Object[] {fallbackMethod, targetMethod, iae}); 
					//configuration errors should be fixed immediately.   
	                throw iae;
	            } catch (InvocationTargetException ite) {
	    			log.debug("got exception while trying the fallbackBean method: '{}'. will try next fallbackBean...",fallbackMethod);
	            	//fallbackBean method thrown an exception. try next bean or throw exception if this is the last bean
	                if (!iter.hasNext()) {
	                	//TODO : do we still need to check isFallbackableException?
	                    throw ite.getCause();
	                }
	            }
	        }
	        //code should never reach this line. 
	        throw t;
		}
	}


    static boolean isFallbackableException(Throwable t,  Class<? extends Throwable>[] fallbackableExceptions) {
    	for (Class<? extends Throwable> throwable : fallbackableExceptions) {
            if (throwable.isAssignableFrom(t.getClass())) {
            	return true;
            }
		}
    	return false;
    }

    @Pointcut
    public abstract void fallbackAnnotatedMethod(Fallback fallbackConfig) ;

    @Pointcut
    public abstract void fallbackAnnotatedClass(Fallback fallbackConfig);

    @Pointcut
    public abstract void fallbackMethodExecution(Fallback fallbackConfig);

}
