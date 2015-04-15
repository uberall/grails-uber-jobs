package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberTriggerMetaService extends AbstractUberService {

    def create(String name, UberJobMeta job, String cronExpression, String queueName, boolean enabled) {
        UberTriggerMeta triggerMeta = new UberTriggerMeta(
                name: name,
                job: job,
                queueName: queueName,
                cronExpression: cronExpression,
                enabled: enabled
        )
        triggerMeta.save(failOnError: true)
    }

    def update(UberTriggerMeta triggerMeta, Map updateParams) {
        UberTriggerMeta.withNewTransaction {
            def locked = UberTriggerMeta.lock(triggerMeta.id)
            if (updateParams.cronExpression) {
                locked.cronExpression = updateParams.cronExpression
            }
            if (updateParams.queueName) {
                locked.queueName = updateParams.queueName
            }
            if (updateParams.enabled) {
                locked.enabled = updateParams.enabled
            }
        }
    }

    def disable(UberTriggerMeta triggerMeta) {
        update(triggerMeta, [enabled: false])
    }

    def enable(UberTriggerMeta triggerMeta) {
        update(triggerMeta, [enabled: true])
    }
}
