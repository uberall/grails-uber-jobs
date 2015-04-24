package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberJobsQueueController extends AbstractUberJobsController {

    static allowedMethods = [list: 'GET', get: 'GET', update: 'PUT', clear: 'GET', delete: 'DELETE']

    def uberJobsQueueService

    def list() {
        renderResponse([list: UberQueue.list(params), total: UberQueue.count])
    }

    def get() {
        withDomainObject(UberQueue) {
            renderResponse([queue: it])
        }
    }

    @Transactional(readOnly = false)
    def update() {
        def json = request.JSON
        if (json.enabled == null) {
            renderBadRequest([error: [enabled: 'NULLABLE']])
        } else if (!(json.enabled in Boolean)) {
            renderBadRequest([error: [enabled: 'INVALID']])
        } else {
            withDomainObject(UberQueue) { UberQueue uberQueue ->
                renderResponse(queue: uberJobsQueueService.update(uberQueue, json.enabled))
            }
        }
    }

    @Transactional(readOnly = false)
    def clear() {
        withDomainObject(UberQueue) {
            uberJobsQueueService.clear(it)
            renderResponse([queue: it])
        }
    }

    @Transactional(readOnly = false)
    def delete() {
        withDomainObject(UberQueue) { uberQueue ->
            if (params.synchronous) {
                uberJobsQueueService.clear(uberQueue)
            } else {
                // enqueue job to clear
            }
            renderResponse([success: true])
        }
    }
}
