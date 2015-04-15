package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberJobMetaService extends AbstractUberService{

    def create(String job, boolean enabled, int minDelay, boolean singletonJob) {
        UberJobMeta result = new UberJobMeta(job: job, enabled: enabled, minDelay: minDelay, singletonJob: singletonJob)
        result.save(failOnError: true)
    }

    def update(UberJobMeta jobMeta, Map updateProperties) {
        def result = null

        // we are using a new transaction and lock the jobMeta row as chances are high that some worker might tried to get this information in the moment of updating
        UberJobMeta.withNewTransaction {
            def locked = UberJobMeta.lock(jobMeta.id)
            if (updateProperties.enabled)
                locked.enabled = updateProperties.enabled
            if (updateProperties.minDely)
                locked.minDelay = updateProperties.minDelay
            if (updateProperties.singletonJob)
                locked.singletonJob = updateProperties.singletonJob
            result = locked.save(flush: true, failOnError: true)
        }
        result
    }

    def disable(UberJobMeta jobMeta){
        update(jobMeta, [enabled: false])
    }

    def enable(UberJobMeta jobMeta){
        update(jobMeta, [enabled: true])
    }


}
