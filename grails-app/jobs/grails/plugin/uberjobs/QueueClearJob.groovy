package grails.plugin.uberjobs

import org.joda.time.DateTime

class QueueClearJob {

    def uberJobsQueueService

    def perform(def id, long until) {
        UberQueue uberQueue = UberQueue.get(id)

        DateTime untilDateTime = null
        if(until > 0)
            untilDateTime = new DateTime(until)
        uberJobsQueueService.clear(uberQueue, untilDateTime)
    }
}