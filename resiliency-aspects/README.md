# Resilience Engineering

Yet Another Resilience Framework inspired by [Netflix Hystrix](https://github.com/Netflix/Hystrix/wiki/How-it-Works)

* Modular, less opinionated API, easy to apply resiliency aspects to existing or new code with no boilerplate code.
* API provided as annotations and builder pattern style DSL, for flexible applicability of aspects at Type, Method-level with annotations and block-level with DSL.
* A set of resilience annotations that can be applied to java/groovy code via AspectJ weaving or Spring proxy based AOP.

###Features
Resiliency Aspects for Java and [JavaScript](https://github.com/xmlking/spa-starter-kit/tree/master/app/scripts/resiliency)

1. **Circuit Breaker** - _Prevent resource saturation, help you build real-time stats driven self-healing micro services._
2. **Fallback** - for graceful degradation.
    1. Failover: _Fail Fast, Fail Silent_
    2. Fallback: _Static, Stubbed, Cache, Secondary service etc.,_
3. **Retry** - _support maxTries, maxDelay, delayRatio, Exponential Backoff Strategy, intermediate callbacks for custom control._
	1. Sample [Code](https://github.com/xmlking/spa-starter-kit/blob/master/app/scripts/reactive/EventBus.js#L73)
4. **Governor** - _resource overload protection, prevent deliberate denial-of-service attacks._
	1. rate-limit
	2. concurrency control
	
> Async , Timeout aspects are deprecated in favour of Grails/Ratpack's Promise (GPars, Reactor), RxJava's Observable API.

###Release
    ./gradlew :resiliency-aspects:jar -PreleaseVersion=1.0.1
 
###Getting Started
1. Keep the released jar file in your project's class path.

2. Let the spring framework discover resilience aspects(AspectJ) and create proxies for your annotated methods.

For Grails projects, set this in `Config.groovy`
```
grails.spring.bean.packages = ['com.crossbusiness.resiliency.aspect.spring']
```
For Spring projects, set this in `applicationContext.xml`
```xml
<context:component-scan base-package="com.crossbusiness.resiliency.aspect.spring"/>
<!-- Enable AspectJ style of Spring AOP -->
<aop:aspectj-autoproxy/>
```

Start using resilience annotations on your service methods. They can be `stackable`‎.

```Groovy
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
```

```Java
@Governor(limit = 1)//implicit type = GovernorType.CONCURRENCY
String throttleWithConcurrency(String arg1) {
    TimeUnit.SECONDS.sleep(5);
    return arg1;
}
@Governor(type = GovernorType.RATE, limit = 3, period = 60L, blocking=true,  unit = TimeUnit.SECONDS)
String throttleWithRateLimitAndBlocking(String arg1) {
    TimeUnit.SECONDS.sleep(5);
    return arg1;
}
```
For more examples see [DemoService](/resiliency-roadshow/grails-app/services/com/crossbusiness/resiliency/demo/DemoService.groovy) in `resiliency-roadshow` sub project.

###Folder Structure for Spring with AspectJ Projects
when using AspectJ weaving (load-time weaving & compile-time weaving)
```
src/
	main/
		java/
			ajia/
				monitoring/
					Monitoring.java
		resources/
			META-INF/
				aop.xml         -> Lib Level config
		webapp/
			WEB-INF/
				web.xml
				applicationContext.xml -> Define spring beans.
				weaverContext.xml -> By using a separate weaverContext.xml file, you can easily enable or disable LTW by including or excluding the file
			META-INF/
			    aop.xml         -> App Level overrides
				context.xml     -> For Tomcat LTW (no -javaagent runtime changes needed)
```

###Testing
User `gradle test` command to run test cases.
Access test results at `Resilience/resiliency-aspects/build/reports/tests/index.html`


###Future
1. [_Promises_](http://www.html5rocks.com/en/tutorials/es6/promises/) and [_Observables_](http://reactivex.io/) /[_Async Generators_](https://github.com/jhusain/asyncgenerator) provider better async code composability and error communication.
2. [Grails Promises](http://grails.org/doc/latest/guide/async.html),  [Spring Reactor Promises](https://github.com/reactor/reactor/wiki/Promises), Java 8 _CompletableFuture_ will eliminate need for _@Async , @Timeout_ aspects.
3. __Event Bus__ is an other way of async code composition ( _Pub-Sub Style_ ) and used for inter-code-fragment communication without blocking  _(Message Passing Pattern)_.

> e.g., [_Vert.X_](http://www.cubrid.org/blog/dev-platform/understanding-vertx-architecture-part-2/) Distributed EventBus that can span [backend-to-frontend](https://riaconnection.wordpress.com/2012/08/04/vert-x-io-event-bus-the-quick-intro/), [_Spring Messaging_](https://github.com/zyro23/grails-spring-websocket) Distributed EventBus,  [_Spring Reactor_](https://github.com/reactor/grails-events), [Guavas](http://www.slideshare.net/koneru9999/guavas-event-bus) EventBuses.
> Some of them  support Point-to-point and Request-Response style messaging with __Ack__ via  _Promises_  API.

4. __RxJava__ and Spring __Reactor Frameworks__ will allow you to switch underling dispatcher(scheduler) implementation of  _Observables_  and _Promises_
with _[calling-thread, thread-pools, NIO,  actors, LMAX Disruptor,  event-loop]_.

> See the Promises examples in [GparsService](/resiliency-roadshow/grails-app/services/com/crossbusiness/resiliency/demo/GparsService.groovy), [DemoService](/resiliency-roadshow/grails-app/services/com/crossbusiness/resiliency/demo/DemoService.groovy).

5. Watch out for [Reactive Streams](https://github.com/reactive-streams/reactive-streams) Spec which is promising to standardize above reactive programming models and support _Backpressure_.

###References

    https://github.com/reactor/reactor/wiki/Promises
    http://gpars.org/1.2.1/guide/guide/single.html
	http://jaxenter.com/tutorial-gpars-making-parallel-systems-groovy-and-java-friendly-104729.html