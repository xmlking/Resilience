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

/**
 * Asynchronous methods must return either void or an implementation of the Future<V> interface. <BR>
 * Asynchronous methods that return void can't declare application exceptions, <BR>
 * but if they return Future<V>, they may declare application exceptions. For example:<BR>
 * @Async<BR>
 * public Future<String> processPayment(Order order) throws PaymentException {<BR>
 *   ... <BR>
 * }<BR>
 * This method will attempt to process the payment of an order, and return the status as a String. <BR>
 * Even if the payment processor takes a long time, the client can continue working, <BR>
 * and display the result when the processing finally completes.<BR><BR>
 *
 *  The org.springframework.scheduling.annotation.AsyncResult<V> class is a concrete implementation <BR>
 *  of the Future<V> interface provided as a helper class for returning asynchronous results. <BR>
 *  AsyncResult has a constructor with the result as a parameter, making it easy to create Future<V> implementations.<BR> 
 *  For example, the processPayment method would use AsyncResult to return the status as a String:<BR><BR>
 
 * @Async
 * public Future<String> processPayment(Order order) throws PaymentException {<BR>
 *   ...<BR>
 *   String status = ...;<BR>
 *   return new AsyncResult<String>(status);<BR>
 * }<BR>
 *
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 * 
 */

@Documented 
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
    /**
     * The semantics of the value element are as follows:<BR>
     * 		If value is a qualifier of a registered Executor(e,g AsyncTaskExecutor) bean, it will use it.<BR>
     * 		If no value is defined or qualifier not found, then it will look for a bean named "defaultExecutor".<BR>
     * 		If "defaultExecutor" is also not found, then it will execute the annotated method in the same thread of the caller<BR><BR>
     *
     */
	String value() default "";
}
