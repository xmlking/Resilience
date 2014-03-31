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
package com.crossbusiness.resiliency.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Use for resource overload protection, prevent deliberate denial-of-service attacks. 
 * Throttle by Concurrency and/or Rate. 
 * TODO Throttle per instance, per method
 * </p>
 * 
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Governor {
	
	public enum GovernorType { 	RATE, CONCURRENCY, ALL };
	
	/**
	 * Throttle Type
	 * default ThrottleType.CONCURRENCY
	 */
    GovernorType type() default GovernorType.CONCURRENCY;
	
	/**
	 * should the caller blocked if threshold is reached?
	 * default false 
	 * for non-blocking scenario: 
	 * 			ConcurrencyLimitExceededException is thrown when threshold is reached for ThrottleType.CONCURRENCY type
	 *	 		RateLimitExceededException is thrown when threshold is reached for ThrottleType.RATE type 
	 */
	boolean blocking() default false;
	
	/**
	 * RATE LIMIT: Total number of "hits" allowed in a period of time in TimeUnits. 
	 * CONCURRENCY LIMIT: Number of "concurrent hits" allowed per instance, per method.
	 * default 15
	 */
    int limit() default 15;
    
	/**
	 * Period of time considered
	 * default 10 MINUTES 
	 */
	long period() default 10 * 60 * 60 * 1000;
	
    /**
     * Time units.
     * default TimeUnit.MILLISECONDS
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
}