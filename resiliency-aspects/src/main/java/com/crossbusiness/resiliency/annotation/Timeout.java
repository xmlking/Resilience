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
 * Makes a method time constrained.
 *
 * <p>For example, this {@code load()} method should not take more than
 * a second, and should be interrupted if it takes more:
 *
 * <pre> &#64;Timeout(limit = 1, unit = TimeUnit.SECONDS)
 * String load(String resource) {
 *   // something that runs potentially long
 * }</pre>
 *
 * <p>Important to note that in Java 1.5+ it is impossible to force thread
 * termination, for many reasons. Thus, we can't
 * just call {@code Thread.stop()},
 * when a thread is over a specified time limit. The best thing we can do is to
 * call {@link Thread#interrupt()} and hope that the thread itself
 * is checking its
 * {@link Thread#isInterrupted()} status. If you want to design your long
 * running methods in a way that {@link Timeout} can terminate them, embed
 * a checker into your most intessively used place, for example:
 *
 * <pre> &#64;Timeout(limit = 1, unit = TimeUnit.SECONDS)
 * String load(String resource) {
 *   while (true) {
 *     if (Thread.currentThread.isInterrupted()) {
 *       throw new IllegalStateException("time out");
 *     }
 *     // execution as usual
 *   }
 * }</pre>
 *
 */
 


/**
 * Specifies the amount of time in a given time unit that a concurrent access attempt should block before timing out.<BR><BR>
 * 
 * The Timeout annotation can be specified on a business method or a bean class. <BR>
 * If it is specified on a class, it applies to all business methods of that class. <BR>
 * If it is specified on both a class and on a business method of the class, the method-level annotation takes precedence for the given method.<BR><BR>
 * 
 *  The semantics of the value element are as follows:<BR>
 *  	A value > 0 indicates a timeout value in the units specified by the unit element.<BR>
 *  	A value of 0 means concurrent access is not permitted.<BR>
 *  	A value of -1 indicates that the client request will block indefinitely until forward progress it can proceed.<BR>
 * 
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 * 
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {
    /**
     * The semantics of the value element are as follows:<BR>
     * 		A value > 0 indicates a timeout value in the units specified by the unit element.<BR>
     * 		A value of 0 means concurrent access is not permitted.<BR>
     * 		A value of -1 indicates that the client request will block indefinitely until forward progress it can proceed.<BR><BR>
     * 
     * 		Values less than -1 are not valid.<BR>
     */
    long value();

    /**
     * Units used for the specified value.
     * 	Default:
     * 		java.util.concurrent.TimeUnit.MILLISECONDS
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
}
