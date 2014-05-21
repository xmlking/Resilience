package com.crossbusiness.resiliency.demo

import grails.web.RequestParameter
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

class GparsController {

    def gparsService

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

    def testMashupMultiplePromisesUsingSpecificPGroups(@RequestParameter('a') String arg1) {
        render gparsService.mashupMultiplePromisesUsingSpecificPGroups(arg1)
    }

    def testSelectFastestResultConcurrently(@RequestParameter('a') String arg1) {
        render gparsService.selectFastestResultConcurrently(arg1)
    }
}
