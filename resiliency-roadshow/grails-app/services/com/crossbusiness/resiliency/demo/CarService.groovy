package com.crossbusiness.resiliency.demo

import com.crossbusiness.resiliency.annotation.Governor
import grails.transaction.Transactional

import java.util.concurrent.TimeUnit

@Governor(type = Governor.GovernorType.RATE, limit = 2, period = 45L,  unit = TimeUnit.SECONDS)
@Transactional(readOnly = true)
class CarService {

    public String serviceMethod(String arg1) {
        return "from CarService ${arg1}"
    }
}
