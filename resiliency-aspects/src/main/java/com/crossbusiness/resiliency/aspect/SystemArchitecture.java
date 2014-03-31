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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.DeclareWarning;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Keep system wide Pointcuts at this central location.
 *
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 */

@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@DeclarePrecedence(
        "com.crossbusiness.resiliency.aspect.AbstractGovernorAspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractFallbackAspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractCircuitBreakerAspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractRetryAspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractTimeoutAspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractTimeout2Aspect+," +
        "com.crossbusiness.resiliency.aspect.AbstractAsyncAspect+")
public abstract class SystemArchitecture {

    @Pointcut("execution(public * *(..))")
    public void publicMethods() {
    }

    // @Pointcut("execution(public * *..*Service.*(..))")
    @Pointcut("bean(*Service) && !bean(userDetailsService) && " +
            "execution(public * *(..))")
    public void publicServiceMethods() {
    }

    @Pointcut("execution(public groovy.lang.MetaClass getMetaClass()) ||"
            + "execution(public void setMetaClass(groovy.lang.MetaClass)) ||"
            + "execution(public Object getProperty(String)) ||"
            + "execution(public void setProperty(String, Object)) ||"
            + "execution(public Object invokeMethod(String, Object))")
    public void groovyObjectMethods() {
    }

    @Pointcut("execution(public * *$*(..))")
    public void groovyDollarSignMethods() {
    }

    @Pointcut("groovyObjectMethods() || groovyDollarSignMethods()")
    public void groovyMOPMethods() {
    }

    @Pointcut("@within(grails.persistence.Entity)")
    public void isDomainClass() {}
}