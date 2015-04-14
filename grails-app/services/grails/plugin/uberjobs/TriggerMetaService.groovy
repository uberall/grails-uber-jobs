package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class TriggerMetaService {

    def create(String name, JobMeta job, String cronExpression, String queueName, boolean enabled) {
        TriggerMeta triggerMeta = new TriggerMeta(
                name: name,
                job: job,
                queueName: queueName,
                cronExpression: cronExpression,
                enabled: enabled
        )
        triggerMeta.save(failOnError: true)
    }

    def update(TriggerMeta triggerMeta, Map updateParams){
        TriggerMeta.withNewTransaction {
            def locked = TriggerMeta.lock(triggerMeta.id)
            if(updateParams.cronExpression) {
                locked.cronExpression = updateParams.cronExpression
            }
            if(updateParams.queueName) {
                locked.queueName = updateParams.queueName
            }
            if(updateParams.enabled) {
                locked.enabled = updateParams.enabled
            }
        }
    }

    def disable(TriggerMeta triggerMeta) {
        update(triggerMeta, [enabled: false])
    }

    def enable(TriggerMeta triggerMeta){
        update(triggerMeta, [enabled: true])
    }
}
