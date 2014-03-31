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
import com.crossbusiness.resiliency.annotation.Timeout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnnotationTimeoutAspect}.
 *
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/11/14.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnnotationTimeoutAspectTest {
    static final Logger log = LoggerFactory.getLogger(AnnotationTimeoutAspectTest.class);

    @Mock
    Delegate delegateMock;

    @InjectMocks
    TestService testService;

    @Test
    public void method_that_takes_1000ms_and_annotated_with_timeout_of_2000ms_will_succeed() throws InterruptedException {
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation)throws InterruptedException {
                Object[] args = invocation.getArguments();
                TimeUnit.MILLISECONDS.sleep(1000);
                return args[0]+" back";
            }
        }).when(delegateMock).mockedMethod(anyString());

        String result = testService.timeout_2000ms("testArg");

        assertEquals("testArg back",result);
        verify(delegateMock).mockedMethod("testArg");
        verifyZeroInteractions(delegateMock);
    }

    @Test(expected = InterruptedException.class)
    public void method_that_takes_2000ms_and_annotated_with_timeout_of_1000ms_will_fail() throws InterruptedException {
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation)throws InterruptedException {
                TimeUnit.MILLISECONDS.sleep(2000);
                return "testArg back";
            }
        }).when(delegateMock).mockedMethod("testArg");

        String result = testService.timeout_1000ms("testArg");
        //Following lines will not be called as InterruptedException will be thrown.
        assertEquals(null, result);
        verify(delegateMock, timeout(2005)).mockedMethod("testArg");
        verifyZeroInteractions(delegateMock);
    }

    @Test
    public void void_method_that_takes_1s_and_annotated_with_timeout_of_2s_and_async_will_get_routed_asynchronously() throws InterruptedException {
        doNothing().when(delegateMock).mockedVoidMethod(anyString());
        testService.async_void_method_taking_1s_will_timeout_in_2s("testArg");

        verify(delegateMock, timeout(1100)).mockedVoidMethod("testArg");
        verifyZeroInteractions(delegateMock);
    }

    @Test
    public void void_method_that_takes_2s_and_annotated_with_timeout_of_1s_and_async_will_get_routed_asynchronously_but_timeout_before_calling_mockedVoidMethod() throws InterruptedException {
        doNothing().when(delegateMock).mockedVoidMethod(anyString());
        testService.async_void_method_taking_2s_will_timeout_in_1s("testArg");

        verify(delegateMock, never()).mockedVoidMethod("testArg");
        verifyZeroInteractions(delegateMock);
    }

    @Component
    private static class TestService {

        @Autowired
        private Delegate delegate;

        @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
        public String timeout_2000ms(String arg) throws InterruptedException {
            return delegate.mockedMethod(arg);
        }

        @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
        public String timeout_1000ms(String arg) throws InterruptedException {
            return delegate.mockedMethod(arg);
        }

        /**
         * Note:  Precedence Order enforced. Timeout(outer), Async(inner){@link SystemArchitecture}.
         */
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @Async
        public void async_void_method_taking_1s_will_timeout_in_2s(String arg) throws InterruptedException {
            log.debug("async_void_method_taking_1s_will_timeout_in_2s() get executed asynchronously, will run in defaultExecutor thread");
            TimeUnit.SECONDS.sleep(1);
            delegate.mockedVoidMethod(arg);
        }

        /**
         * Note:  Precedence Order enforced. Timeout(outer), Async(inner){@link SystemArchitecture}.
         */
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @Async
        public void async_void_method_taking_2s_will_timeout_in_1s(String arg) throws InterruptedException {
            log.debug("async_void_method_taking_2s_will_timeout_in_1s() get executed asynchronously, will run in defaultExecutor thread");
            TimeUnit.SECONDS.sleep(2);
            delegate.mockedVoidMethod(arg);
        }

        /**
         * It should raise an error "Only methods that are declared with throws InterruptedException
         * may have an @Timeout annotation". This method must remain commented-out, otherwise there
         * will be a compile-time error. Uncomment to manually verify that the compiler produces an
         * error message due to the 'declare error' statement in
         * {@link AnnotationTimeoutAspect}.
         */
//        @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
//        public String timeout_compile_error(String arg) {
//            return delegate.mockedMethod(arg);
//        }

    }

    private static interface Delegate {
        String mockedMethod(String arg);
        void mockedVoidMethod(String arg);
    }
}