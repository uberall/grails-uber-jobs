package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional(readOnly = true)
class UberQueueController extends AbstractUberController {

    def uberQueueService

    def list() {
        def list = UberQueue.createCriteria().list {
            if(!params.getBoolean('includeEmpty'))
                sizeGt('items', 0)
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
        withDomainObject(UberQueue)  {uberQueue->
            if(params.synchronous){
                uberQueueService.clear(uberQueue)
            } else {
                // enqueue job to clear
            }
            renderResponse([success: true])
        }
    }
}
