package grails.plugin.uberjobs

import grails.transaction.Transactional
import org.joda.time.DateTime

@Transactional
class UberJobsTriggerMetaService extends AbstractUberService {

    def uberJobsSchedulingService

    def create(String name, UberJobMeta job, String cronExpression, String queueName, List arguments, boolean enabled, boolean failOnError = true) {
        UberTriggerMeta triggerMeta = new UberTriggerMeta(
                name: name,
                job: job,
                queueName: queueName,
                cronExpression: cronExpression,
                enabled: enabled,
                lastFired: DateTime.now()
        )
        triggerMeta.arguments = arguments
        triggerMeta.save(failOnError: failOnError)
        if (triggerMeta) {
            uberJobsSchedulingService.updateThreadsNextExecution(triggerMeta.estimatedNextExecution)
        }
        triggerMeta
    }

    def update(UberTriggerMeta triggerMeta, Map updateParams) {
        def result = null
        UberTriggerMeta.withNewTransaction {
            def locked = UberTriggerMeta.lock(triggerMeta.id)
            if (updateParams.cronExpression) {
                locked.cronExpression = updateParams.cronExpression
            }
            if (updateParams.queueName) {
                locked.queueName = updateParams.queueName
            }
            if (updateParams.enabled != null) {
                locked.enabled = updateParams.enabled
            }
            if (updateParams.name) {
                locked.name = updateParams.name
            }
            if (updateParams.arguments) {
                triggerMeta.arguments = updateParams.arguments
            }
            result = locked.save(failOnError: true, flush: true)
        }
        if (result) {
            uberJobsSchedulingService.updateThreadsNextExecution(triggerMeta.estimatedNextExecution)
        }
        result
    }

    def delete(UberTriggerMeta triggerMeta) {
        triggerMeta.job.removeFromTriggers(triggerMeta)
        triggerMeta.delete()
    }

    def disable(UberTriggerMeta triggerMeta) {
        update(triggerMeta, [enabled: false])
    }

    def enable(UberTriggerMeta triggerMeta) {
        update(triggerMeta, [enabled: true])
    }
}
