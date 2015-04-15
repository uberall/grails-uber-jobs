package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberWorkerService extends AbstractUberService {

    def uberQueueService

    def createWorkersFromConfig() {
        config.workers.each { poolName, config ->
            config.workers.times { index ->
                List queueNames
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
                start(poolName, index, queueNames)
            }
        }
    }

    def start(poolName, index, queueNames){
        UberWorkerMeta workerMeta = UberWorkerMeta.findByPoolNameAndHostnameAndIndex(poolName, hostName, index)
        if(!workerMeta){

        }
    }

    def createWorker(String poolName, int index, List queueNames) {
        def queues = []
        queueNames.each { String name
            queues << uberQueueService.findOrCreate(name)
        }
        true
    }


    String getName(poolName, index) {
        "$hostName-$poolName-$index"
    }
}
