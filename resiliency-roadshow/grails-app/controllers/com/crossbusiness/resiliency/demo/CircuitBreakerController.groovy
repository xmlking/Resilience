package com.crossbusiness.resiliency.demo

//import com.crossbusiness.resiliency.aspect.CircuitBreakerAspect

import com.crossbusiness.resiliency.aspect.spring.AnnotationCircuitBreakerAspect
import com.crossbusiness.resiliency.aspect.CircuitBreakerRegistryEntry


class CircuitBreakerController {

    AnnotationCircuitBreakerAspect annotationCircuitBreakerAspect

    static defaultAction = "list"

    def list = {

        Map<String, CircuitBreakerRegistryEntry> circuitBreakersMap
        Throwable e

        try {
            circuitBreakersMap = annotationCircuitBreakerAspect.circuitBreakersMap
        } catch(Throwable ex) {
            e = ex
        }

        [circuitBreakersMap:circuitBreakersMap, error:e]
    }

    def show = {

        CircuitBreakerRegistryEntry  circuitBreaker
        Throwable e

        try {
            Map<String, CircuitBreakerRegistryEntry> circuitBreakersMap = annotationCircuitBreakerAspect.circuitBreakersMap
            circuitBreaker = circuitBreakersMap.get(params.id)
        } catch(Throwable ex) {
            e = ex
        }

        [circuitBreaker:circuitBreaker, error:e]
    }
}
