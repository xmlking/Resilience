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

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.crossbusiness.resiliency.annotation.Governor;

import com.crossbusiness.resiliency.exception.ConcurrencyLimitExceededException;
import com.crossbusiness.resiliency.exception.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a Semaphore with the same permits as the count per period of the throttle (eg: 2, in a 2 invocations per second throttle)
 * Create a Timer that runs as the throttle period and resets all the initial permits of the semaphore (eg: each second)
 * Before each invocation, just try to acquire the semaphore, if there are not permits available, it will block until the timer resets the permits and the method can proceed
 */
/**
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 *
 */
//@Aspect("pertarget(@annotation(throttleConfig))") //TODO to keep 'throttles' unique. This makes one aspect instance for each unique target object at matched join points. Standard @Aspect makes it singleton by default.
@Aspect
public abstract class AbstractGovernorAspect {
	static final Logger log = LoggerFactory.getLogger(AbstractGovernorAspect.class);
	
	private interface Throttler {
		public Object proceed(ProceedingJoinPoint pjp) throws Throwable;
	}

	private static class ConcurrencyThrottleDecorator implements Throttler{
		int limit;
		Semaphore semaphore;
		boolean blocking;
		
		public ConcurrencyThrottleDecorator(int limit, boolean blocking) {
			this.limit = limit;
			this.blocking = blocking;
			this.semaphore = new Semaphore(limit, true);
		}
		@Override
		public Object proceed(ProceedingJoinPoint pjp) throws Throwable {
			if(blocking) {
				try {
					semaphore.acquire();
					return pjp.proceed();
				}finally {
					semaphore.release();
				} 
			} else {
				if (semaphore.tryAcquire()) {
					try {
						return pjp.proceed();
					} finally {
						semaphore.release();
					}
				} else {
					log.warn("Request rejected: concurrency limit {} exceeded", limit);
					throw new ConcurrencyLimitExceededException(limit);
				}				
			}
		}
	}
	private static class RateThrottleDecorator extends ConcurrencyThrottleDecorator{
		ScheduledFuture timer;
		
		public RateThrottleDecorator(final int limit, final long period, final TimeUnit unit, final boolean blocking, ScheduledExecutorService scheduler) {
			super(limit,blocking);
			this.timer = scheduler.scheduleAtFixedRate( new TimerTask()
			{
			     public void run()
			     {
						log.debug("Throttler[limit={},period={},unit={},blocking={}] : {} permits available before release", 
								new Object[] {limit, period, unit, blocking, semaphore.availablePermits()});
						synchronized(semaphore) {
							semaphore.release(limit - semaphore.availablePermits());
						}
			     }
			} , 0, period, unit);
		}
		
		public Object proceed(ProceedingJoinPoint pjp) throws Throwable {
			if(timer.isCancelled()) {
				throw new RuntimeException("Invalid timer!");
			}
			if(blocking) {
				semaphore.acquire();
				return pjp.proceed();
			} else {
				if(semaphore.tryAcquire()) {
					return pjp.proceed();
				} else {
					log.warn("Request rejected: rate limit {} exceeded", limit);
					throw new RateLimitExceededException(limit);					
				}
			}
		}
	}
	
	Map<String, Throttler> throttles = new ConcurrentHashMap<String, Throttler>();
	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Around("governorAnnotatedClass(governorConfig)")
    public Object governorOnClassLevel(final ProceedingJoinPoint pjp, Governor governorConfig) throws Throwable {
        return wrap(pjp,governorConfig);
    }

    @Around("governorAnnotatedMethod(governorConfig)")
    public Object governorOnMethodLevel(final ProceedingJoinPoint pjp, Governor governorConfig) throws Throwable {
        return wrap(pjp,governorConfig);
    }

    // @Around("governorMethodExecution(governorConfig)")
    protected Object wrap(final ProceedingJoinPoint pjp, Governor governorConfig) throws Throwable {
		// throttle per instance, per method
		String throttleKey = pjp.getTarget().hashCode() + pjp.getSignature().toLongString();
		
		if(!throttles.containsKey(throttleKey)) {
			switch (governorConfig.type()) {
			case CONCURRENCY:
					throttles.put(throttleKey, new ConcurrencyThrottleDecorator(governorConfig.limit(), governorConfig.blocking()));
					break;
			case RATE: 
					throttles.put(throttleKey, new RateThrottleDecorator(governorConfig.limit(), governorConfig.period(),
								governorConfig.unit(), governorConfig.blocking(), scheduler));
					break;
			case ALL: //TODO
					break;
			default: 
					log.error("Unknown Throttler type");
					break;
			}
		}
		
		Throttler t = throttles.get(throttleKey);
		if(t != null) {
			return t.proceed(pjp);
		} else {
			log.error("no throttles found");
			return pjp.proceed();
		}

    }
    
	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();
	}

    @Pointcut
    public abstract void governorAnnotatedMethod(Governor governorConfig);

    @Pointcut
    public abstract void governorAnnotatedClass(Governor governorConfig);

    @Pointcut
    public abstract void governorMethodExecution(Governor governorConfig);
}
