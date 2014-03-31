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

import com.crossbusiness.resiliency.annotation.Fallback;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/16/14.
 */
@Component
@Order(112)
@Aspect 
public class AnnotationFallbackAspect extends AbstractFallbackAspect {

    /**
     * Matches the execution of any method with the @{@link com.crossbusiness.resiliency.annotation.Fallback} annotation.
     */
    @Pointcut("execution(@com.crossbusiness.resiliency.annotation.Fallback * *(..)) && @annotation(fallbackConfig)")
    public void fallbackAnnotatedMethod(Fallback fallbackConfig) {}

    /**
     * Matches the execution of any public method in a type with the @{@link com.crossbusiness.resiliency.annotation.Fallback}
     * annotation, or any subtype of a type with the {@code Fallback} annotation.
     */
    @Pointcut("execution(public * ((@com.crossbusiness.resiliency.annotation.Fallback *)+).*(..)) " +
            "&& within(@com.crossbusiness.resiliency.annotation.Fallback *) && @target(fallbackConfig) " +
            "&& !com.crossbusiness.resiliency.aspect.SystemArchitecture.groovyMOPMethods()")
    public void fallbackAnnotatedClass(Fallback fallbackConfig) {}

    /**
     * Definition of pointcut from super aspect - matched join points
     * will have Fallback Aspect applied.
     */
    @Pointcut("(fallbackAnnotatedMethod(fallbackConfig) || fallbackAnnotatedClass(fallbackConfig))")
    public void fallbackMethodExecution(Fallback fallbackConfig) { }
}
