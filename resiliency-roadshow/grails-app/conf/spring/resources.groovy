// Place your Spring DSL code here
beans = {
    //importBeans 'file:grails-app/conf/spring/resiliency.groovy'

    xmlns task:"http://www.springframework.org/schema/task"

    // Executor Pools
    task.executor(id:'defaultExecutor', 'pool-size':'5-10', 'queue-capacity':'25', 'rejection-policy':'CALLER_RUNS')
    task.executor(id:'SumoThreadPool', 'pool-size':'5-10', 'queue-capacity':'25', 'rejection-policy':'CALLER_RUNS')
    task.executor(id:'TwoThreadPool', 'pool-size':'1-2', 'queue-capacity':'1')

    tarService(com.crossbusiness.resiliency.demo.TarService)
}