# Resilience Engineering

Yet Another Resilience Framework inspired by [Netflix Hystrix](https://github.com/Netflix/Hystrix/wiki/How-it-Works)

Netflix Hystrix framework is great for using with new projects but it may not be suitable for adopting to existing/legacy projects.
We created a set of resilience annotations that can be applied to any java projects via AspectJ weaving or Spring proxy based AOP.

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
Reactor Framework allows to switch underling dispatcher implementation for Promises from event-loop(single threaded) to thread pools(multi threaded).
See the Promises examples in [DemoService](/resiliency-roadshow/grails-app/services/com/crossbusiness/resiliency/demo/DemoService.groovy).

###References

    https://github.com/reactor/reactor/wiki/Promises
    http://gpars.org/1.1.0/guide/guide/single.html