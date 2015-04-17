package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberTriggerMetaController extends AbstractUberController {

    static allowedMethods = [list: 'GET', get: 'GET', update: 'PUT', create: 'POST', delete: 'DELETE']

    def uberTriggerMetaService

    def list() {
        def list = UberTriggerMeta.list(params)
        renderResponse([list: list, total: UberTriggerMeta.count])
    }

    def get() {
        withDomainObject(UberTriggerMeta) { trigger ->
            renderResponse([trigger: trigger])
        }
    }

    @Transactional(readOnly = false)
    def update() {
        withDomainObject(UberTriggerMeta) { UberTriggerMeta triggerMeta ->
            def result = uberTriggerMetaService.update(triggerMeta, request.JSON as Map)
            if (!result.validate()) {
                renderErrorResponse(result)
            } else {
                renderResponse([trigger: result])
            }
        }
    }

    @Transactional(readOnly = false)
    def create() {
        def json = request.JSON
        UberJobMeta jobMeta = UberJobMeta.read(json.job)
        // TODO: check for validtiy
        def result = uberTriggerMetaService.create(json.name, jobMeta, json.cronExpression, json.queueName, json.enabled, false)
        if (!result.validate()) {
            renderErrorResponse(result)
        } else {
            renderResponse([trigger: result])
        }
    }

    @Transactional(readOnly = false)
    def delete() {
        withDomainObject(UberTriggerMeta) { UberTriggerMeta triggerMeta ->
            uberTriggerMetaService.delete(triggerMeta)
            renderResponse([success: true])
        }
    }


}
