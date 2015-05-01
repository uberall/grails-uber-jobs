package grails.plugin.uberjobs

import grails.transaction.Transactional
import org.joda.time.DateTime

@Transactional
class UberJobsQueueService extends AbstractUberJobsService {

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

    /**
     * Set whether the given queue should be enabled or disabled.
     * No worker will aquire jobs from this queue if it is disabled.
     * The queue will be locked while its status is being updated.
     *
     * @param uberQueue the queue to enabled or disabled
     * @param enabled true if the queue should be enabled, false if it shoud be disabled
     */
    void update(UberQueue uberQueue, boolean enabled) {
        UberQueue.withNewTransaction {
            def locked = UberQueue.lock(uberQueue.id)
            locked.enabled = enabled
            uberQueue.save(failOnError: true, flush: true)
        }
    }

    /**
     * Disables the given queue. No worker will grab jobs from this queue.
     *
     * @param uberQueue the queue to disable
     */
    void disable(UberQueue uberQueue) {
        update(uberQueue, false)
    }

    /**
     * Enables the given queue. Workers will grab jobs from this queue.
     *
     * @param uberQueue the queue to enable
     */
    def enabled(UberQueue uberQueue) {
        update(uberQueue, true)
    }

    /**
     * Clears the specified queue of any OPEN jobs where the execution time is in the past.
     *
     * @param uberQueue the queue to clear jobs from
     * @return the number of cleared jobs
     */
    int clear(UberQueue uberQueue){
        UberJob.where { queue == uberQueue && status == UberJob.Status.OPEN && doAt < DateTime.now() }.deleteAll()
    }
}
