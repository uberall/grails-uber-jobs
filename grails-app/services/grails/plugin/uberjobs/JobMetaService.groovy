package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class JobMetaService {

    def create(String job, boolean enabled, int minDelay, boolean singletonJob) {
        JobMeta result = new JobMeta(job: job, enabled: enabled, minDelay: minDelay, singletonJob: singletonJob)
        result.save(failOnError: true)
    }

    def update(JobMeta jobMeta, Map updateProperties) {
        def result = null

        // we are using a new transaction and lock the jobMeta row as chances are high that some worker might tried to get this information in the moment of updating
        JobMeta.withNewTransaction {
            def locked = JobMeta.lock(jobMeta.id)
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

    def disable(JobMeta jobMeta){
        update(jobMeta, [enabled: false])
    }

    def enable(JobMeta jobMeta){
        update(jobMeta, [enabled: true])
    }


}
