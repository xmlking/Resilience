package com.crossbusiness.resiliency.demo

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.dataflow.Select

import javax.annotation.PostConstruct

import static groovyx.gpars.dataflow.Dataflow.*

import grails.transaction.Transactional

@Transactional
class GparsService {
    def grailsApplication
    def sumoPGroup, twoPGroup, defaultPGroup

    @PostConstruct
    def init() {
        sumoPGroup = new DefaultPGroup(new DefaultPool(grailsApplication.mainContext.SumoThreadPool.threadPoolExecutor))
        twoPGroup = new DefaultPGroup(new DefaultPool(grailsApplication.mainContext.TwoThreadPool.threadPoolExecutor))
        defaultPGroup = new DefaultPGroup(new DefaultPool(grailsApplication.mainContext.defaultExecutor.threadPoolExecutor))
    }

    String mashupMultiplePromisesUsingSpecificPGroups(String arg1) {

        Promise allPromise

        Dataflow.usingGroup(sumoPGroup) { //sumoPGroup.with {

            Promise echo = task {
                log.debug('In echo task: this log should be in sumo pool')
                "http://echo.jsontest.com/YouSaid/${arg1}".toURL().text
            }
            Promise ip = task {
                log.debug('In ip task: this log should be in sumo pool')
                "http://ip.jsontest.com/".toURL().text
            }
            Promise time = task {
                log.debug('In time task: this log should be in sumo pool')
                "http://time.jsontest.com/".toURL().text
            }
            Promise md5 = twoPGroup.task {  //override the default group by being specific
                log.debug('In md5 task: this log should be in two pool')
                "http://md5.jsontest.com/?text=${arg1}".toURL().text
            }

            allPromise = whenAllBound(echo, ip, time, md5) { echoResult, ipResult, timeResult, md5Result ->
                log.debug "in allPromise: this log should be in sumo pool"
                echoResult + ipResult + timeResult + md5Result
            }

        }

        return  allPromise.val
    }

    String selectFastestResultConcurrently(String arg1) {

        final alt
        Dataflow.usingGroup(sumoPGroup) {

            Promise time1 = task {
                log.debug('In time1 task: this log should be in sumo pool')
                "http://www.timeapi.org/utc/now".toURL().text
            }
            Promise time2 = task {
                log.debug('In time2 task: this log should be in sumo pool')
                "http://api.geonames.org/timezoneJSON?formatted=true&lat=47.01&lng=10.2&username=demo&style=full".toURL().text
            }
            Promise time3 = defaultPGroup.task {
                log.debug('In time3 task: this log should be in default pool')
                "http://time.jsontest.com/".toURL().text
            }

            final timeoutChannel = Select.createTimeout(500)

            alt = new Select(sumoPGroup, time1, time2, time3, timeoutChannel)

        }

        return alt.select()
    }

}
