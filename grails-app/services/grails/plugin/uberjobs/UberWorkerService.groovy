package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberWorkerService extends AbstractUberService {

    def uberQueueService
    def uberWorkerMetaService

    def createWorkersFromConfig() {
        // iterate over workers configurations
        config.workers.each { poolName, config ->
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
            queueNames.each { String name
                queues << uberQueueService.findOrCreate(name)
            }
            // start the appropiate amount of workers
            config.workers.times { index ->
                start(poolName, index, queues)
            }
        }
    }

    def start(poolName, index, queues){
        UberWorkerMeta workerMeta = UberWorkerMeta.findByPoolNameAndHostnameAndIndex(poolName, hostName, index)
        if(!workerMeta){
            workerMeta = createWorker(poolName, index, queues)
        } else if(config.workers.update){
            // TODO: UPDATE
        }
        //TODO: start the actual Thread
    }

    def createWorker(String poolName, int index, List queues) {
        uberWorkerMetaService.create(poolName, index, queues)
    }


    String getName(poolName, index) {
        "$hostName-$poolName-$index"
    }
}
