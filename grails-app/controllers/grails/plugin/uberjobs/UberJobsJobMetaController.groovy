package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberJobsJobMetaController extends AbstractUberJobsController {

    static allowedMethods = [list: 'GET', get: 'GET', update: 'PUT']

    def uberJobsJobMetaService

    def list() {
        def list = UberJobMeta.list(params)
        renderResponse([list: list, total: UberJobMeta.count])
    }

    def get() {
        withDomainObject(UberJobMeta) { jobMeta ->
            renderResponse([jobMeta: jobMeta])
        }
    }

    @Transactional(readOnly = false)
    def update() {
        withDomainObject(UberJobMeta) { UberJobMeta jobMeta ->
            UberJobMeta result = uberJobsJobMetaService.update(jobMeta, request.JSON as Map)
            if(result.validate()) {
                renderResponse(jobMeta: result)
            } else {
                renderErrorResponse(result)
            }
        }
    }
}