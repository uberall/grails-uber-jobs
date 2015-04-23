package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberJobsQueueController extends AbstractUberJobsController {

    static allowedMethods = [list: 'GET', get: 'GET', update: 'PUT', clear: 'GET', delete: 'DELETE']

    // TODO: allowed methods
    def uberQueueService

    def list() {
        def list = UberQueue.createCriteria().list(params) {
            if (!params.getBoolean('includeEmpty')) {
                items {
                    eq("status", UberJob.Status.OPEN)
                }
            }
        }
        renderResponse([list: list, total: list.totalCount])
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
                renderResponse(queue: uberQueueService.update(uberQueue, json.enabled))
            }
        }
    }

    @Transactional(readOnly = false)
    def clear() {
        withDomainObject(UberQueue) {
            uberQueueService.clear(it)
            renderResponse([queue: it])
        }
    }

    @Transactional(readOnly = false)
    def delete() {
        withDomainObject(UberQueue) { uberQueue ->
            if (params.synchronous) {
                uberQueueService.clear(uberQueue)
            } else {
                // enqueue job to clear
            }
            renderResponse([success: true])
        }
    }
}
