package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberJobMetaController extends AbstractUberController {

    static allowedMethods = [list: 'GET', get: 'GET', update: 'PUT']

    def uberJobMetaService

    def list() {
        def list = UberJobMeta.list(params)
        renderResponse([list: list, total: list.totalCount])
    }

    def get() {
        withUberJobMeta { jobMeta ->
            renderResponse([jobMeta: jobMeta])
        }
    }

    @Transactional(readOnly = false)
    def update() {
        withUberJobMeta { UberJobMeta jobMeta ->
            UberJobMeta result = uberJobMetaService.update(jobMeta, params)
            if(result.validate()) {
                renderResponse(jobMeta: jobMeta)
            } else {
                renderErrorResponse(result)
            }
        }
    }

    private void withUberJobMeta(Closure closure) {
        UberJobMeta jobMeta = UberJobMeta.get(params.getLong('id'))
        if (!jobMeta) {
            renderNotFound()
        } else {
            closure.call jobMeta
        }
    }
}