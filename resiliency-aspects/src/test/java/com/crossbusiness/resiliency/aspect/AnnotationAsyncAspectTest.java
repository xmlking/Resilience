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

import java.util.concurrent.*;

import org.aspectj.lang.Aspects;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;

import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.crossbusiness.resiliency.annotation.Async;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.*;
//import static org.mockito.BDDMockito.*;
//TODO Mockito sample check: TransactionManagementAspectTest in book page 383
//http://samerabdelkafi.wordpress.com/2013/07/01/junit-test-with-mockito-and-spring/
//http://gojko.net/2009/10/23/mockito-in-six-easy-examples/

//public class AsyncAspectTest extends TestCase {
//}
/**
 * Unit tests for {@link AnnotationAsyncAspectTest}.
 *
 * Created by Sumanth Chinthagunta <xmlking@gmail.com> on 3/11/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/applicationContext.xml", "/META-INF/executors.xml", "/META-INF/weaverContext.xml"})
public class AnnotationAsyncAspectTest {
    static final Logger log = LoggerFactory.getLogger(AnnotationAsyncAspectTest.class);

    @Autowired
    Executor defaultExecutor;

    @Before
    public void setUp() {
        //MockitoAnnotations.initMocks(this);

        /* uncomment this line if you want to test with SimpleAsyncTaskExecutor
           default is @Autowired AsyncTaskExecutor (i.e., defaultExecutor).
         */
        //defaultExecutor = new SimpleAsyncTaskExecutor();
        Aspects.aspectOf(AnnotationAsyncAspect.class).setExecutor(defaultExecutor);
    }



    @Test
    public void async_method_gets_routed_asynchronously() {
        ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
        obj.incrementAsync();
        awite(500);
        assertEquals(1, obj.counter);
    }

    @Test
    public void async_method_returning_future_gets_routed_asynchronously_and_returns_a_future() throws InterruptedException, ExecutionException {
        ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
        assertEquals(5, obj.incrementReturningAFuture().get().intValue());
        assertEquals(1, obj.counter);
    }

    @Test
    public void sync_method_gets_routed_synchronously() {
        ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
        obj.increment();
        assertEquals(1, obj.counter);
    }

    @Test
    public void void_method_in_async_class_gets_routed_asynchronously() {
        ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
        obj.increment();
        awite(500);
        assertEquals(1, obj.counter);
    }

    @Test
    public void method_returning_future_in_async_class_gets_routed_asynchronously_and_returns_a_future() throws InterruptedException, ExecutionException {
        ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
        Future<Integer> future = obj.incrementReturningAFuture();
        assertEquals(5, future.get().intValue());
        assertEquals(1, obj.counter);
    }


	@Test
	public void method_returning_non_void_non_future_in_async_class_gets_routed_synchronously() {
		ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
		int returnValue = obj.return5();
		assertEquals(5, returnValue);
	}

    @Test
    public void qualified_async_methods_are_routed_to_correct_executor() throws InterruptedException, ExecutionException {

        ClassWithQualifiedAsyncMethods obj = new ClassWithQualifiedAsyncMethods();

        Future<Thread> sumoThread = obj.sumoThreadPoolWork();
        assertThat(sumoThread.get(), not(Thread.currentThread()));
        assertThat(sumoThread.get().getName(), startsWith("sumoThreadPool"));

        Future<Thread> twoPoolThread = obj.twoThreadPoolWork();
        assertThat(twoPoolThread.get().getName(), startsWith("twoThreadPool"));
    }

    @Component
    static class ClassWithoutAsyncAnnotation {
        int counter;

        @Async public void incrementAsync() {
            counter++;
            log.debug("incrementAsync() get executed asynchronously, will run in defaultExecutor or SimpleAsyncTaskExecutor");
        }

        public void increment() {
            counter++;
            log.debug("increment() get executed asynchronously, will run in defaultExecutor or SimpleAsyncTaskExecutor");
        }

        @Async public Future<Integer> incrementReturningAFuture() {
            counter++;
            log.debug("incrementReturningAFuture() get executed asynchronously, will run in defaultExecutor or SimpleAsyncTaskExecutor");
            return new AsyncResult<Integer>(5);
        }

        /**
         * It should raise an error to attach @Async to a method that returns a non-void
         * or non-Future. This method must remain commented-out, otherwise there will be a
         * compile-time error. Uncomment to manually verify that the compiler produces an
         * error message due to the 'declare error' statement in
         * {@link AnnotationAsyncAspectTest}.
         */
//		@Async public int getInt() {
//			return 0;
//		}
    }

    @Component
    @Async("sumoThreadPool")
    static class ClassWithAsyncAnnotation {
        int counter;

        public void increment() {
            log.debug("increment() method get executed asynchronously, will run in sumoThreadPool");
            counter++;
        }

        // Manually check that there is a warning from the 'declare warning' statement in AsyncAspectTest
		public int return5() {
            log.debug("return5() method get executed synchronously, will not be run in sumoThreadPool");
			return 5;
		}

        public Future<Integer> incrementReturningAFuture() {
            counter++;
            log.debug("incrementReturningAFuture() get executed asynchronously,  will run in sumoThreadPool");
            return new AsyncResult<Integer>(5);
        }
    }

    @Component
    static class ClassWithQualifiedAsyncMethods {
        @Async("sumoThreadPool")
        public Future<Thread> sumoThreadPoolWork() {
            log.debug("sumoThreadPoolWork() will get executed in sumoThreadPool");
            return new AsyncResult<Thread>(Thread.currentThread());
        }

        @Async("twoThreadPool")
        public Future<Thread> twoThreadPoolWork() {
            log.debug("twoThreadPoolWork() will get executed in twoThreadPool");
            return new AsyncResult<Thread>(Thread.currentThread());
        }
    }

   static private void awite(int time) {
       try {
           TimeUnit.MILLISECONDS.sleep(time);
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
   }
}