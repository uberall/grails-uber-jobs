package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberQueueService extends AbstractUberService {

    def findOrCreate(String name){
        def result = UberQueue.findByName(name)
        if(!result){
            result = create(name)
        }
        result
    }

    def create(String name) {
        new UberQueue(name: name, enabled: true).save(failOnError: true)
    }

    def update(UberQueue uberQueue, boolean enabled) {
        UberQueue.withNewTransaction {
            def locked = UberQueue.lock(uberQueue.id)
            locked.enabled = enabled
            uberQueue.save(failOnError: true, flush: true)
        }
    }

    def disable(UberQueue uberQueue) {
        update(uberQueue, false)
    }

    def enabled(UberQueue uberQueue) {
        update(uberQueue, true)
    }
}
