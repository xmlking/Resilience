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


import com.crossbusiness.resiliency.annotation.Async;
import com.crossbusiness.resiliency.annotation.CircuitBreaker;
import com.crossbusiness.resiliency.exception.OpenCircuitException;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.googlecode.catchexception.CatchException.*;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessage;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasNoCause;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/11/14.
 */
/**
 * Unit tests for {@link AnnotationCircuitBreakerAspect}.
 *
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/11/14.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnnotationCircuitBreakerAspectTest extends TestCase {
    static final Logger log = LoggerFactory.getLogger(AnnotationCircuitBreakerAspectTest.class);

    @Mock
    Delegate delegateMock;

    @InjectMocks
    TestService testService;

    @InjectMocks
    ClassWithCircuitBreakerAnnotation classWithCircuitBreakerAnnotation;

    @Test(expected = OpenCircuitException.class)
    public void method_annotated_with_circuitBreaker_that_fails_2times_within_60000ms_will_throw_OpenCircuitException() {

        log.debug("Starting Test : method_annotated_with_circuitBreaker_that_fails_2times_within_60000ms_will_throw_OpenCircuitException");

        when(delegateMock.mockedMethod(anyString()))
            .thenReturn("testArg back")
            .thenThrow(new RuntimeException("first fake RuntimeException"))
            .thenThrow(new RuntimeException("second fake RuntimeException"));

        testService.failure2_threshold60000l("testArg"); //First Time
        catchException(testService).failure2_threshold60000l("testArg"); //Second Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(RuntimeException.class)),
                hasMessage("first fake RuntimeException"),
                hasNoCause()
            )
        );
        awite(1000);
        catchException(testService).failure2_threshold60000l("testArg"); //Third Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(RuntimeException.class)),
                hasMessage("second fake RuntimeException"),
                hasNoCause()
            )
        );
        testService.failure2_threshold60000l("testArg"); //Forth Time
        //Following lines will not be reached as OpenCircuitException will be thrown.
        verify(delegateMock, timeout(2000).times(3)).mockedMethod("testArg");
    }

    @Test
    public void method_annotated_with_circuitBreaker_that_fails_2times_with_2100ms_delay_should_pass() {

        log.debug("Starting Test : method_annotated_with_circuitBreaker_that_fails_2times_with_2100ms_delay_should_pass");

        when(delegateMock.mockedMethod(anyString()))
            .thenReturn("testArg back")
            .thenThrow(new IllegalStateException("first fake IllegalStateException"))
            .thenThrow(new IllegalStateException("second fake IllegalStateException"));


        testService.failure2_threshold2000l("testArg"); //First Time

        catchException(testService).failure2_threshold2000l("testArg"); //Second Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(IllegalStateException.class)),
                hasMessage("first fake IllegalStateException"),
                hasNoCause()
            )
        );

        awite(2100);

        catchException(testService).failure2_threshold2000l("testArg"); //Third Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(IllegalStateException.class)),
                hasMessage("second fake IllegalStateException"),
                hasNoCause()
            )
        );

        catchException(testService).failure2_threshold2000l("testArg"); //Forth Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(IllegalStateException.class)),
                hasMessage("second fake IllegalStateException"),
                hasNoCause()
            )
        );

        verify(delegateMock, timeout(3000).times(4)).mockedMethod("testArg");
    }

    @Test
    public void method_annotated_with_circuitBreaker_that_fails_2times_within_2000ms_with_NPE_should_pass() {

        log.debug("Starting Test : method_annotated_with_circuitBreaker_that_fails_2times_within_2000ms_with_NPE_should_pass");

        when(delegateMock.mockedMethod(anyString())) //NullPointerException
                .thenReturn("testArg back")
                .thenThrow(new NullPointerException("first fake NullPointerException"))
                .thenThrow(new NullPointerException("second fake NullPointerException"));

        testService.failure2_threshold2000l("testArg"); //First Time

        catchException(testService).failure2_threshold2000l("testArg"); //Second Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(NullPointerException.class)),
                hasMessage("first fake NullPointerException"),
                hasNoCause()
            )
        );

        awite(100);

        catchException(testService).failure2_threshold2000l("testArg"); //Third Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(NullPointerException.class)),
                hasMessage("second fake NullPointerException"),
                hasNoCause()
            )
        );

        catchException(testService).failure2_threshold2000l("testArg"); //Forth Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(NullPointerException.class)),
                hasMessage("second fake NullPointerException"),
                hasNoCause()
            )
        );

        verify(delegateMock, timeout(500).times(4)).mockedMethod("testArg");
    }

    @Test
    public void method_annotated_with_circuitBreaker_that_fails_2times_within_2000ms_but_tried_after_3000ms_should_pass() {

        log.debug("Starting Test : method_annotated_with_circuitBreaker_that_fails_2times_within_2000ms_but_tried_after_3000ms_should_pass");

        when(delegateMock.mockedMethod(anyString()))
                .thenReturn("testArg back")
                .thenThrow(new RuntimeException("first fake RuntimeException"))
                .thenThrow(new RuntimeException("second fake RuntimeException"));

        testService.failure2_threshold2000l_retryAfter3000l("testArg"); //First Time

        catchException(testService).failure2_threshold2000l_retryAfter3000l("testArg"); //Second Time
        assertThat(caughtException(),
            allOf(
                        is(instanceOf(RuntimeException.class)),
                        hasMessage("first fake RuntimeException"),
                        hasNoCause()
                )
        );

        awite(500);

        catchException(testService).failure2_threshold2000l_retryAfter3000l("testArg"); //Third Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(RuntimeException.class)),
                hasMessage("second fake RuntimeException"),
                hasNoCause()
            )
        );

        catchException(testService).failure2_threshold2000l_retryAfter3000l("testArg"); //Forth Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(OpenCircuitException.class)),
                hasMessage("open circuit"),
                hasNoCause()
            )
        );

        awite(3100);

        catchException(testService).failure2_threshold2000l_retryAfter3000l("testArg"); //Fifth Time
        assertThat(caughtException(),
            allOf(
                is(instanceOf(RuntimeException.class)),
                hasMessage("second fake RuntimeException"),
                hasNoCause()
            )
        );

        verify(delegateMock, timeout(4000).times(4)).mockedMethod("testArg");
    }

    @Test(expected = OpenCircuitException.class)
    public void class_annotated_with_circuitBreaker_that_fails_2times_within_60000ms_will_throw_OpenCircuitException() {

        log.debug("Starting Test : class_annotated_with_circuitBreaker_that_fails_2times_within_60000ms_will_throw_OpenCircuitException");

        when(delegateMock.mockedMethod(anyString()))
                .thenReturn("testArg back")
                .thenThrow(new RuntimeException("first fake RuntimeException"))
                .thenThrow(new RuntimeException("second fake RuntimeException"));

        classWithCircuitBreakerAnnotation.class_failure2_threshold60000l("testArg"); //First Time
        catchException(classWithCircuitBreakerAnnotation).class_failure2_threshold60000l("testArg"); //Second Time
        assertThat(caughtException(),
                allOf(
                        is(instanceOf(RuntimeException.class)),
                        hasMessage("first fake RuntimeException"),
                        hasNoCause()
                )
        );
        awite(500);
        catchException(classWithCircuitBreakerAnnotation).class_failure2_threshold60000l("testArg"); //Third Time
        assertThat(caughtException(),
                allOf(
                        is(instanceOf(RuntimeException.class)),
                        hasMessage("second fake RuntimeException"),
                        hasNoCause()
                )
        );
        classWithCircuitBreakerAnnotation.class_failure2_threshold60000l("testArg"); //Forth Time
        //Following lines will not be reached as OpenCircuitException will be thrown.
        verify(delegateMock, timeout(2000).times(3)).mockedMethod("testArg");
    }

    @Component
    private static class TestService {

        @Autowired
        private Delegate delegate;

        @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l)
        public String failure2_threshold60000l(String arg) throws OpenCircuitException {
            return delegate.mockedMethod(arg);
        }

        @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=2000l,failureIndications={IllegalStateException.class})
        public String failure2_threshold2000l(String arg) throws OpenCircuitException {
            return delegate.mockedMethod(arg);
        }

        @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=2000l,retryAfterMs=3000l)
        public String failure2_threshold2000l_retryAfter3000l(String arg) throws OpenCircuitException {
            return delegate.mockedMethod(arg);
        }

        /**
         * Note:  Precedence Order enforced. CircuitBreaker(outer), Fallback(inner){@link SystemArchitecture}.
         */
        //@Fallback(value = ['barService','jarService'], exceptions = {OpenCircuitException.class,FileNotFoundException.class})
        @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l,retryAfterMs=80000l,failureIndications={FileNotFoundException.class})
        public void async_void_method_taking_1000ms_will_timeout2_in_2000ms(String arg) throws OpenCircuitException {
            awite(1000);
            delegate.mockedVoidMethod(arg);
        }

        /**
         * It should raise an error "Only methods that are declared with throws OpenCircuitException
         * may have an @CircuitBreaker annotation". This method must remain commented-out, otherwise there
         * will be a compile-time error. Uncomment to manually verify that the compiler produces an
         * error message due to the 'declare error' statement in
         * {@link AnnotationCircuitBreakerAspect}.
         */
//        @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l,retryAfterMs=80000l)
//        public String circuitBreaker_compile_error(String arg) {
//            return delegate.mockedMethod(arg);
//        }

    }

    @Component
    @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l)
    private static class ClassWithCircuitBreakerAnnotation {
        @Autowired
        private Delegate delegate;

        public String class_failure2_threshold60000l(String arg) throws OpenCircuitException {
            return delegate.mockedMethod(arg);
        }

        /**
         * It should raise an error "Only methods that are declared with throws OpenCircuitException
         * may have an @CircuitBreaker annotation". This method must remain commented-out, otherwise there
         * will be a compile-time error. Uncomment to manually verify that the compiler produces an
         * error message due to the 'declare error' statement in
         * {@link AnnotationCircuitBreakerAspect}.
         */
//        public String circuitBreaker_compile_error(String arg) {
//            return delegate.mockedMethod(arg);
//        }
    }

    private static interface Delegate {
        String mockedMethod(String arg);
        void mockedVoidMethod(String arg);
    }

    static private void awite(int time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
