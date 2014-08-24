# Resilience Engineering

Yet Another Resilience Framework inspired by [Netflix Hystrix](https://github.com/Netflix/Hystrix/wiki/How-it-Works)

* Modular, less opinionated API, easy to apply resiliency aspects to existing or new code with no boilerplate code.
* API provided as annotations and builder pattern style DSL, for flexible applicability of aspects at Type, Method-level with annotations and block-level with DSL.
* A set of resilience annotations that can be applied to java/groovy code via AspectJ weaving or Spring proxy based AOP.

###Features
Resiliency Aspects for Java and JavaScript

1. **Circuit Breaker** - _Real-time stats drive, self-healing micro services._
2. **Fallback** - _failover, graceful degradation_
3. **Retry** - _support maxTries, maxDelay, delayRatio, Backoff Strategy, intermediate callback._
4. **Governor** - _resource overload protection, prevent deliberate denial-of-service attacks._
	1. rate-limit
	2. concurrency control
	
> Async , Timeout aspects are deprecated with Grails/Ratpack's Promise (GPars, Reactor), RxJava's Observable API.

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
Grails Promises with Reactor plugin will eliminate need for @Async , @Timeout aspects.
Reactor Framework allows to switch underling dispatcher implementation of Promises from event-loop(single threaded) to thread pools(multi threaded).
See the Promises examples in [DemoService](/resiliency-roadshow/grails-app/services/com/crossbusiness/resiliency/demo/DemoService.groovy).

###References

    https://github.com/reactor/reactor/wiki/Promises
    http://gpars.org/1.2.1/guide/guide/single.html
