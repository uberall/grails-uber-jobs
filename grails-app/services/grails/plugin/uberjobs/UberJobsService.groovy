package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberJobsService extends AbstractUberService {

    def uberJobMetaService
    def uberTriggerMetaService
    def uberQueueService

    /**
     * scans all "UberJobArtefacts" and updates/creates meta information for Jobs, Triggers and Queues
     * @return
     */
    def init() {
        def jobClasses = grailsApplication.uberJobClasses
        jobClasses.each { GrailsUberJobClass jobClass ->
            initJob(jobClass)
            initQueue(jobClass.defaultQueueName)
        }

        if (config.jobs.cleanup) {
            def jobList = UberJobMeta.list()
            def namesFromDB = jobList.job
            def jobNames = jobClasses.name
            (namesFromDB - jobNames).each { name ->
                log.info "pruning information for JobMeta -> $name"
                jobList.find { it.job == name }.delete()
                // TODO: we will need to delete triggers referencing this job
            }
        }

        if (config.triggers.cleanup) {
            def triggerList = UberTriggerMeta.list()
            def namesFromDB = triggerList.name
            def triggerNames = jobClasses.triggers.collect {
                return it.keySet()
            }.flatten()
            (namesFromDB - triggerNames).each { name ->
                log.info "pruning information for TriggerMeta -> $name"
                uberTriggerMetaService.delete(triggerList.find { it.name == name })
            }
        }
    }

    private initQueue(String queueName) {
        UberQueue queue = UberQueue.findByName(queueName)
        if (!queue) {
            log.info "creating Queue $queueName"
            uberQueueService.create(queueName)
        }
    }

    private initJob(GrailsUberJobClass job) {
        def name = job.name
        log.debug("handling JobMeta for $job")
        UberJobMeta jobMeta = UberJobMeta.findByJob(name)
        if (!jobMeta) {
            jobMeta = uberJobMetaService.create(name, true, job.minDelay, job.singletonJob)
            log.info("created JobMeta for $job")
        } else if (config.jobs.update) {
            jobMeta = uberJobMetaService.update(jobMeta, [enabled: true, minDelay: job.minDelay, singletonJob: job.singletonJob])
            log.info("updated JobMeta for $job")
        }

        log.debug("handling Triggers for $job")
        def triggers = job.getTriggers()
        triggers.each { triggerName, settingsExpando ->
            Map settings = settingsExpando.getProperty('triggerAttributes')
            log.info("handling trigger $job.name -> $triggerName with settings: $settings")
            UberTriggerMeta triggerMeta = UberTriggerMeta.findByName(triggerName)
            if (!triggerMeta) {
                uberTriggerMetaService.create(triggerName, jobMeta, settings.cronExpression, settings.queueName, true)
            } else if (config.triggers.update) {
                settings.enabled = true
                uberTriggerMetaService.update(triggerMeta, settings)
            }
        }
    }

}