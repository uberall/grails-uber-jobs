package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberJobMetaService extends AbstractUberService {

    def create(String job, boolean enabled, int minDelay, boolean singletonJob, failOnError = true) {
        UberJobMeta result = new UberJobMeta(job: job, enabled: enabled, minDelay: minDelay, singletonJob: singletonJob)
        result.save(failOnError: failOnError)
        result
    }

    def update(UberJobMeta jobMeta, Map updateProperties, failOnError = true) {
        def result = null

        // we are using a new transaction and lock the jobMeta row as chances are high that some worker might tried to get this information in the moment of updating
        UberJobMeta.withNewTransaction {
            def locked = UberJobMeta.lock(jobMeta.id)
            if (updateProperties.enabled != null)
                locked.enabled = updateProperties.enabled
            if (updateProperties.minDely != null)
                locked.minDelay = updateProperties.minDelay
            if (updateProperties.singletonJob != null)
                locked.singletonJob = updateProperties.singletonJob
            result = locked
            locked.save(flush: true, failOnError: failOnError)
        }
        result
    }

    def disable(UberJobMeta jobMeta) {
        update(jobMeta, [enabled: false])
    }

    def enable(UberJobMeta jobMeta) {
        update(jobMeta, [enabled: true])
    }


}
