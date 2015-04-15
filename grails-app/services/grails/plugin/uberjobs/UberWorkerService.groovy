package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberWorkerService extends AbstractUberService {

    def uberQueueService
    def uberWorkerMetaService

    private static final List WORKERS = []

    def createWorkersFromConfig() {
        // iterate over workers configurations
        config.workers.each { String poolName, config ->
            if (poolName in ['update', 'cleanup', 'restart']) {
                return
            }
            List queueNames
            // check queue configuration, should be a list, a single string or a closure that is returning a list
            if (config.queueNames in List) {
                queueNames = config.queueNames
            } else if (config.queueNames instanceof Closure) {
                def closureResult = config.queueNames.call()
                assert closureResult in List, "$poolName's queueNames closure has not returned a list!"
                queueNames = closureResult
            } else if (config.queueNames in String) {
                queueNames = [config.queueNames]
            } else {
                throw new RuntimeException("queueNames of $poolName must be a List, Closure (returning a list) or a String. ${config.queueNames.class.simpleName} is not allowed!")
            }
            def queues = []
            queueNames.each { name ->
                queues << uberQueueService.findOrCreate(name.toString())
            }
            // start the appropiate amount of workers
            config.workers.times { int index ->
                start(poolName, index, queues)
            }
        }
    }

    def shutdown(){
        log.info("Shutting down ${UberWorkerMeta.countByHostnameAndStatusNotEqual(hostName, UberWorkerMeta.Status.STOPPED)}")
        WORKERS.each { worker ->
            //TODO: it.end()
        }
        boolean allDone = false
        int attempts = 10
        while(!allDone){
            sleep(1000)
            int notStopped = UberWorkerMeta.countByHostnameAndStatusNotEqual(hostName, UberWorkerMeta.Status.STOPPED)
            log.info("Shutdown attempt ${attempts-9} of $attempts -> $notStopped Workers has not beed stopped by now.")
            allDone = notStopped == 0 || attempts == 1
            attempts--
        }
    }

    def start(String poolName, int index, List<UberQueue> queues) {
        UberWorkerMeta workerMeta = UberWorkerMeta.findByPoolNameAndHostnameAndIndex(poolName, hostName, index)
        if (!workerMeta) {
            workerMeta = uberWorkerMetaService.create(poolName, index, queues)
        } else if (config.workers.update) {
            // TODO: UPDATE
        }
        //TODO: start the actual Thread and add it to the list of workers
    }

    String getName(poolName, index) {
        "$hostName-$poolName-$index"
    }
}
