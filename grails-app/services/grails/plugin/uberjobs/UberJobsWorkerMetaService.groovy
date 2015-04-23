package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberJobsWorkerMetaService extends AbstractUberJobsService{

    def create(String poolName, int index, List<UberQueue> queues) {
        UberWorkerMeta workerMeta = new UberWorkerMeta(poolName: poolName, index: index, hostname: hostName, status:  UberWorkerMeta.Status.STARTING)
        queues.each {
            workerMeta.addToQueues(it)
        }
        workerMeta.save(failOnError: true)
    }
}
