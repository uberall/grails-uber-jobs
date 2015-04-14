package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberJobsService {

    def jobMetaService
    def triggerMetaService
    def grailsApplication


    def init(){
        def jobClasses = grailsApplication.uberJobClasses
        jobClasses.each{ GrailsUberJobClass jobClass ->
            initJob(jobClass)
        }

        if(config.jobs.cleanup){
            def jobList = JobMeta.list()
            def namesFromDB = jobList.job
            def jobNames = jobClasses.name
            (namesFromDB - jobNames).each { name ->
                jobList.find{it.job == name}.delete()
                // TODO: we will need to delete triggers referencing this job
            }
        }

        if(config.triggers.cleanup){
            def triggerList = TriggerMeta.list()
            def namesFromDB = triggerList.name
            def triggerNames = jobClasses.trigger.name
        }
    }

    private initJob(GrailsUberJobClass job) {
        def name = job.name
        log.debug("handling JobMeta for $job")
        JobMeta jobMeta = JobMeta.findByJob(name)
        if (!jobMeta) {
            jobMeta = jobMetaService.create(name, true, job.minDelay, job.singletonJob)
            log.info("created JobMeta for $job")
        } else if (config.jobs.update) {
            jobMeta = jobMetaService.update(jobMeta, [enabled: true, minDelay: job.minDelay, singletonJob: job.singletonJob])
            log.info("updated JobMeta for $job")
        }

        log.debug("handling Triggers for $job")
        def triggers = job.getTriggers()
        triggers.each { triggerName, settingsExpando ->
            Map settings = settingsExpando.getProperty('triggerAttributes')
            log.info("handling trigger $job.name -> $triggerName with settings: $settings")
            TriggerMeta triggerMeta = TriggerMeta.findByName(triggerName)
            if (!triggerMeta) {
                triggerMetaService.create(triggerName, jobMeta, settings.cronExpression, settings.queueName, true)
            } else if (config.triggers.update) {
                settings.status = TriggerMeta.Status.ENABLED
                triggerMetaService.update(triggerMeta, settings)
            }
        }
    }

    def getConfig() {
        grailsApplication.config.grails.uberjobs
    }
}