package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberWorkerMetaService extends AbstractUberService{

    def create(String poolName, int index, List<UberQueue> queues) {
        UberWorkerMeta workerMeta = new UberWorkerMeta(poolName: poolName, index: index, hostname: hostName)
        queues.each {
            workerMeta.addToQueues(it)
        }
        workerMeta.save(failOnError: true)
    }
}
