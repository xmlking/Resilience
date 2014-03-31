package com.crossbusiness.resiliency.demo

import grails.async.PromiseList
import grails.transaction.Transactional
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.Pool
import groovyx.gpars.scheduler.ResizeablePool
import org.grails.async.factory.gpars.GparsPromise

import javax.annotation.PostConstruct
import java.util.concurrent.TimeoutException

import static grails.async.Promises.*
import grails.async.Promise
import com.crossbusiness.resiliency.annotation.*
import com.crossbusiness.resiliency.exception.*
import com.crossbusiness.resiliency.annotation.Governor.GovernorType
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.annotation.AsyncResult

@Transactional(readOnly = true)
class DemoService {
    def grailsApplication

    //def group1 = new DefaultPGroup(new ResizeablePool(true))
    //def group2 = new DefaultPGroup(2)
    def SumoThreadPoolGroup, TwoThreadPoolGroup

    @PostConstruct
    def init() {
        SumoThreadPoolGroup = new DefaultPGroup(new DefaultPool(grailsApplication.mainContext.SumoThreadPool.threadPoolExecutor))
        TwoThreadPoolGroup = new DefaultPGroup(new DefaultPool(grailsApplication.mainContext.TwoThreadPool.threadPoolExecutor))
    }

    String asyncFlowWithPromise(String arg1){

        List results = new ArrayList();
        //http://www.jsontest.com/
        Promise p1 = task { "http://echo.jsontest.com/YouSaid/${arg1}".toURL().text  }
        Promise p2 = task { "http://md5.jsontest.com/?text=${arg1}".toURL().text }
        Promise p3 = task { "http://ip.jsontest.com/".toURL().text }
        Promise p4 = task { "http://time.jsontest.com/".toURL().text }
        Promise p5 = task { "http://validate.jsontest.com/?json=${arg1}".toURL().text }

        onComplete([p3,p4]) { List p3p4 ->
            results << p3p4
        }.then { p3p4 ->
            println p3p4
            p3p4 << waitAll(p1, p2, p5)
        }.get(3,TimeUnit.SECONDS)
}

    String asyncFlowWithPromisesUsingControlledThreadPools(String arg1) {
        Promise p1 = new GparsPromise(SumoThreadPoolGroup.task {
            log.debug('this log should be in sumo pool')
            "http://echo.jsontest.com/YouSaid/${arg1}".toURL().text
        })
        Promise p2 = new GparsPromise(TwoThreadPoolGroup.task {
            log.debug('this log should be in two pool')
            "http://md5.jsontest.com/?text=${arg1}".toURL().text
        })
        waitAll(p1, p2)
    }

    private Future<String> await(String arg1, int count) {
        log.debug "beginning await work for ${count} seconds..."
        int loop = 0
        try{
            //do batch jobs incrementally
            while(!Thread.currentThread().isInterrupted() & loop < count){ //TODO: checking isInterrupted is really useful?
                //do a sub-task of a long running batch job here
                TimeUnit.SECONDS.sleep(1)  // fake working for one second
                loop++
                log.debug "loop count ... ${loop}"
            }
        }catch(InterruptedException ie){
            log.debug "got Interrupted!... loop count ... ${loop}, Cleaning the resource..."
            //do cleanup job... close connections etc...
            Thread.currentThread().interrupt() // TADA: this will keep not swallowing interrupt flag
        }
        log.debug "loop count Out ... ${loop}"
        return new AsyncResult<String>("REPLY: ${arg1} looped: ${loop} times")
    }

    @Async('SumoThreadPool')
    Future<String> asyncWithSumoThreadPool(String arg1) {
        return await(arg1, 6);
    }

    @Retry(attempts = 2, delay =8L, unit = TimeUnit.SECONDS, exceptions = [TaskRejectedException.class])
    // TwoThreadPool Executor uses default rejection-policy i.e., AbortPolicy
    // Throws TaskRejectedException if more then 3 concurrent request hit.
    @Async("TwoThreadPool")
    Future<String> retryOnFailureWithTwoThreadPool(String arg1) throws TaskRejectedException{
        return await(arg1 , 8);
    }

    @Timeout(value = 3l, unit = TimeUnit.SECONDS)
    String timeout(String arg1) throws InterruptedException{
        def time = new Random().nextInt(3)+2 // time will be either 2 , 3 or 4
        log.debug "waiting in testTimeout1 ${time}..."
        TimeUnit.SECONDS.sleep(time)
        return arg1
    }

    @Timeout2(value = 3010l, unit = TimeUnit.MILLISECONDS)
    String timeout2(String arg1) throws TimeoutException{
        def time = new Random().nextInt(3)+2 // time will be either 2 , 3 or 4
        log.debug "waiting in testTimeout2 ${time}..."
        TimeUnit.SECONDS.sleep(time)
        return arg1
    }

    //implicit type = ThrottleType.CONCURRENCY
    @Governor(limit = 1)
    String throttleWithConcurrency(String arg1) {
        TimeUnit.SECONDS.sleep(5)
        return arg1
    }

    @Governor(limit = 1, blocking=true)
    String throttleWithConcurrencyAndBlocking(String arg1) {
        TimeUnit.SECONDS.sleep(5)
        return arg1
    }

    @Governor(type = GovernorType.RATE, limit = 2, period = 45L,  unit = TimeUnit.SECONDS)
    String throttleWithRateLimit(String arg1) {
        TimeUnit.SECONDS.sleep(5)
        return arg1
    }

    @Governor(type = GovernorType.RATE, limit = 3, period = 60L, blocking=true,  unit = TimeUnit.SECONDS)
    String throttleWithRateLimitAndBlocking(String arg1) {
        TimeUnit.SECONDS.sleep(5)
        return arg1
    }

    // Apply @Retry only to Idempotent Operations
    @Retry(attempts = 2, delay =8L, unit = TimeUnit.SECONDS, exceptions = [IOException.class,RateLimitExceededException.class])
    String retryOnFailure(String arg1) {
        if (new Random().nextInt(5) == 4) throw new IOException()
        // need to call this way to go through CGLIB Proxy.
        return grailsApplication.mainContext.demoService.throttleWithRateLimit(arg1)
    }



    @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l,retryAfterMs=80000l)
    String circuitBreaker(String arg1) {
        TimeUnit.SECONDS.sleep(2)
        if (new Random().nextInt(3) == 2)   throw new FileNotFoundException("fake FileNotFoundException")
        return "Hello  ${arg1} in testCircuitBreaker"
    }

    @Fallback(value = ['barService','jarService'], exceptions = [OpenCircuitException.class,FileNotFoundException.class])
    @CircuitBreaker(failureThreshold=2,failureThresholdTimeFrameMs=60000l,retryAfterMs=80000l,failureIndications=[FileNotFoundException.class])
    String greet() {
        TimeUnit.SECONDS.sleep(2)
        throw new FileNotFoundException()
        return "greet: Hello! in DemoService.greet()"
    }

}
