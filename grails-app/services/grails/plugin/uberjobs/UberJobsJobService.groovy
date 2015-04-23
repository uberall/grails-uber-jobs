package grails.plugin.uberjobs

import grails.transaction.Transactional
import org.joda.time.DateTime

@Transactional
class UberJobsJobService extends AbstractUberJobsService {

    def uberJobsJobMetaService
    def uberJobsTriggerMetaService
    def uberJobsQueueService

    def enqueue(String job, List arguments, String queue = null, DateTime at = DateTime.now()){
        def jobClass = grailsApplication.uberJobClasses.find{it.fullName == job}
        assert jobClass, "No Job found for name $job"
        enqueue(jobClass.clazz as Class, arguments, queue, at)
    }

    def enqueue(Class job, List arguments, String queue = null, DateTime at = DateTime.now()){
        def jobClass = grailsApplication.uberJobClasses.find{it.fullName == job.canonicalName}
        UberJobMeta jobMeta = UberJobMeta.findByJob(jobClass.fullName)
        UberQueue uberQueue = uberJobsQueueService.findOrCreate(queue ?: jobClass.defaultQueueName)
        UberJob uberJob = new UberJob(
                job: jobMeta,
                queue: uberQueue,
                doAt: at,
                status: UberJob.Status.OPEN,
        )
        uberJob.arguments.addAll(arguments)
        if(uberJob.validate()){
            uberJob.save()
        }else {
            log.error("UberJob could not be validated: $uberJob.errors.allErrors")
        }
        uberJob
    }

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
            def jobNames = jobClasses.fullName
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
                uberJobsTriggerMetaService.delete(triggerList.find { it.name == name })
            }
        }
    }

    private initQueue(String queueName) {
        UberQueue queue = UberQueue.findByName(queueName)
        if (!queue) {
            log.info "creating Queue $queueName"
            uberJobsQueueService.create(queueName)
        }
    }

    private initJob(GrailsUberJobClass job) {
        def name = job.fullName
        log.debug("handling JobMeta for $job")
        UberJobMeta jobMeta = UberJobMeta.findByJob(name)
        if (!jobMeta) {
            jobMeta = uberJobsJobMetaService.create(name, true, job.minDelay, job.singletonJob)
            log.info("created JobMeta for $job")
        } else if (config.jobs.update) {
            jobMeta = uberJobsJobMetaService.update(jobMeta, [enabled: true, minDelay: job.minDelay, singletonJob: job.singletonJob])
            log.info("updated JobMeta for $job")
        }

        log.debug("handling Triggers for $job")
        def triggers = job.getTriggers()
        triggers.each { triggerName, settingsExpando ->
            Map settings = settingsExpando.getProperty('triggerAttributes')
            log.info("handling trigger $job.name -> $triggerName with settings: $settings")
            UberTriggerMeta triggerMeta = UberTriggerMeta.findByName(triggerName.toString())
            if (!triggerMeta) {
                uberJobsTriggerMetaService.create(triggerName.toString(), jobMeta, settings.cronExpression as String, settings.queueName as String, (settings.args ?: []) as List, true)
            } else if (config.triggers.update) {
                settings.enabled = true
                uberJobsTriggerMetaService.update(triggerMeta, settings)
            }
        }
    }

}