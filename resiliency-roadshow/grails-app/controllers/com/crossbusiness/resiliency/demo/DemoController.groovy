package com.crossbusiness.resiliency.demo

import grails.async.PromiseList
import grails.async.PromiseMap
import grails.web.RequestParameter
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

import org.springframework.core.task.TaskRejectedException
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

import java.lang.reflect.UndeclaredThrowableException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import com.crossbusiness.resiliency.exception.*

import static grails.async.Promises.task;

class DemoController {

    def demoService
    def fooService
    def jarService
    def tarService
    def carService

    def index() {

        def actions = new HashSet<String>()
        def actionName
        def controllerClass = grailsApplication.getArtefactInfo(ControllerArtefactHandler.TYPE).getGrailsClassByLogicalPropertyName(controllerName)
        controllerClass.getURIs().each { uri ->
            actionName = controllerClass.getMethodActionName(uri)
            if (actionName != "index") actions.add(actionName)
        }
        [actions:actions]
    }

    @MessageMapping("/hello")
    @SendTo("/topic/log")
    protected String hello() {
        return "hello from controller!"
    }

    def testAsyncWithPromise(@RequestParameter('a') String arg1) {

        def symbols = ['AAPL', 'GOOG', 'IBM', 'MSFT']

        def promiseList = new PromiseList()
        symbols.each { stock ->
            promiseList << task {
                def url = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=${stock}&f=nsl1op&e=.csv")
                //log.debug("I will be running in a thread pool")
                Double price = url.text.split(',')[-1] as Double
            }
        }
        def prices = promiseList.get(2,TimeUnit.SECONDS)
        render ([symbols, prices].transpose().inject([:]) { a, b -> a[b[0]] = b[1]; a })
    }

    def testMashupAsyncTasksUsingGrailsPromises(@RequestParameter('a') String arg1) {
        render demoService.mashupAsyncTasksUsingGrailsPromises(arg1)
    }

    def testAsyncTasksUsingGrailsPromisesAndControlledThreadPools(@RequestParameter('a') String arg1) {
        render demoService.asyncTasksUsingGrailsPromisesAndControlledThreadPools(arg1)
    }

/** Calls to get(...) result in a java.util.concurrent.CancellationException being thrown.
 * If the invocation resulted in an exception during processing by the service bean,
 * calls to get(...) result in a java.util.concurrent.ExecutionException being thrown.
 * The cause of the ExecutionException may be retrieved by calling the ExecutionException.getCause method.
 * If the timeout value is exceeded, a java.util.concurrent.TimeoutException is thrown.
 **/
    def testAsyncWithSumoThreadPool(@RequestParameter('a') String arg1) {
        Future<String> futureResult = demoService.asyncWithSumoThreadPool(arg1)// client makes this an async call
        String result
        try {
            result = futureResult.get(7, TimeUnit.SECONDS)
        }catch (TimeoutException te) {
            // handle the timeout
            log.debug  "Timeout....Trying to cancel the worker thread..."
            log.debug  "Cancellation was successful? : ${futureResult.cancel(true)}"
        } catch (ExecutionException ee) {
            // task completed with error, handle appropriately
            log.debug  "ExecutionException...."
            throw ee.getCause()
        } catch (InterruptedException e) {
            // handle the interrupts caused by caller of this method, while waiting
            log.debug  "InterruptedException...."
        }catch (CancellationException ce) {
            // task has been cancelled by others, handle appropriately
            log.debug  "CancellationException...."
        } finally {
//			futureResult.cancel(true); // may or may not desire this
        }

        log.debug  "returning the result...."
        if(futureResult.isCancelled()){
            log.debug "Work cancelled"
            render "Work cancelled"
        }
        else if(futureResult.isDone()){
            log.debug "work completed..."
            render result
        } else {
            render "Work still going on....."
        }
    }

    // May throw TaskRejectedException if more then 3 concurrent request hit.
    def testRetryOnFailureWithTwoThreadPool(@RequestParameter('a') String arg1) {
        Future<String> futureResult
        String result
        try {
            futureResult = demoService.retryOnFailureWithTwoThreadPool(arg1)
            result = futureResult.get() //futureResult.get(6, TimeUnit.SECONDS)
        }catch (TimeoutException te) {
            // handle the timeout
            log.debug  "Timeout...Trying to cancel the worker thread..."
            log.debug  "Cancellation was successful? : ${futureResult.cancel(true)}"
        } catch (ExecutionException ee) {
            // task completed with error, handle appropriately
            log.debug  "ExecutionException....",ee
            throw ee.getCause()
        } catch (TaskRejectedException tre) {
            // task rejected due to overload, handle appropriately
            // This should not happen as service will retry when it receive this exception.
            log.debug  "TaskRejectedException...."
            throw tre
        } catch(InterruptedException | CancellationException e ) {
            // handle the interrupts the cancellations caused by caller of this method, while waiting
            log.debug  "Interrupted or Cancelled ....${e.message}\n\t\tTrying to cancel the worker thread..."
            log.debug  "\t\tCancellation was successful? : ${futureResult.cancel(true)}"
        } finally {
//			futureResult.cancel(true); // may or may not desire this
        }

        log.debug  "returning the result...."
        if(futureResult.isCancelled()){
            log.debug "Work cancelled"
            render "Work cancelled"
        }
        else if(futureResult.isDone()){
            log.debug "work completed..."
            render result
        } else {
            render "Work still going on....."
        }
    }

    def testTimeout(@RequestParameter('a') String arg1) {
        try {
            render demoService.timeout(arg1)
        }catch (InterruptedException ie) {
            // handle the interrupts
            log.debug  "InterruptedException...."
            render "request timeout...."
        }
    }

    def testTimeout2(@RequestParameter('a') String arg1) {
        try {
            render demoService.timeout2(arg1)
        }catch (TimeoutException te) {
            // handle the timeout
            log.debug  "TimeoutException...."
            render "request timeout...."
        }
    }


    def testGovernorWithConcurrency(@RequestParameter('a') String arg1) {
        try{
            render demoService.throttleWithConcurrency(arg1)
        } catch(ConcurrencyLimitExceededException e) {
            log.warn("ConcurrencyLimitExceededException : ${e.message}")
            render "Please try again later :${e.message} "
        }
    }

    def testGovernorWithConcurrencyAndBlocking(@RequestParameter('a') String arg1) {
        render demoService.throttleWithConcurrencyAndBlocking(arg1)
    }

    def testGovernorWithRateLimit(@RequestParameter('a') String arg1) {
        try{
            render demoService.throttleWithRateLimit(arg1)
        }catch(RateLimitExceededException e) {
            log.warn("RateLimitExceededException : ${e.message}")
            render "Please try again later :${e.message} "
        }
    }

    def testGovernorWithRateLimitAndBlocking(@RequestParameter('a') String arg1) {
        render demoService.throttleWithRateLimitAndBlocking(arg1)
    }


    def testGovernorWithRateLimitOnClass(@RequestParameter('a') String arg1) {
        try{
            render carService.serviceMethod(arg1)
        }catch(RateLimitExceededException e) {
            log.warn("RateLimitExceededException : ${e.message}")
            render "Please try again later :${e.message} "
        }
    }

    def testRetryOnFailure(@RequestParameter('a') String arg1) {
        try{
            render demoService.retryOnFailure(arg1)
        }catch(RateLimitExceededException rlee) {
            render "You are trying too hard.   I can't Fallback any more.<br/>Error : ${rlee}"
        }catch(Throwable tro) {
            render "You are trying too hard.   I can't Retry any more.<br/>Error : ${tro}"//${tro.undeclaredThrowable}"
        }

    }

    def testFallback(@RequestParameter('a') String arg1) {
        try {
            render fooService.greet() + fooService.salute()
        }  catch(Throwable tro) {
            render "You are trying too hard.   I can't Fallback any more.<br/>Error : ${tro}"
        }
    }



    def testCircuitBreaker(@RequestParameter('a') String arg) {
        try {
            render "Ok ...${demoService.circuitBreaker(arg)}"
        }catch(OpenCircuitException oce) {
            render "Error : Circuit Open. Please wait for 80 seconds"
        } catch(Throwable tro) {
            Throwable t = tro instanceof UndeclaredThrowableException ? tro.undeclaredThrowable : tro
            render "Error : ${t.toString()}"
            //render "Error : ${tro}"
        }
    }

    def testCircuitBreakerWithFallback(@RequestParameter('a') String arg1) {
        try {
            render demoService.greet()
        }catch(Throwable tro) {
            render "Error : ${tro}"
        }

    }

    def handleTaskRejectedException(TaskRejectedException e) {
        render 'Got TaskRejectedException...'
    }
    def handleTimeoutException(TimeoutException e) {
        render 'Got TimeoutException...'
    }
}
