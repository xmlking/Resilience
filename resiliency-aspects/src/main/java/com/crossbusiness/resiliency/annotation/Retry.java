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
 * Retry Failed Idempotent Operations <BR>
 * idempotent concurrent operation is a good candidate for retrying upon failure.
 * example Exception   ConcurrencyFailureException, PessimisticLockingFailureException
 * Ref: http://josiahgore.blogspot.com/2011/02/using-spring-aop-to-retry-failed.html 
 * 
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 * 
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Retry {

    /**
     * How many times to retry.
     */
    int attempts() default 2;

    /**
     * Delay between attempts, in time units.
     */
    long delay() default 500;

    /**
     * Time units.
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * Failure Indications
     * When to retry (in case of what exception types).
     * Specify exceptions for which operation should be retried.
     * @todo  exclude exception as failure
     */
    Class<? extends Exception>[] exceptions() default {Exception.class };

    /**
     * Shall it be fully verbose (show full exception trace) or just
     * exception message?
     */
    boolean verbose() default true;

}
