package grails.plugin.uberjobs

class UberJobsWorkerMetaController extends AbstractUberJobsController {

    static allowedMethods = [list: 'GET', get: 'GET', create: 'POST', pause: 'PUT', stop: 'PUT', resume: 'PUT']

    def uberJobsWorkerService

    def list() {
        def list = UberWorkerMeta.createCriteria().list(params) {
            if (!params.getBoolean("includeIdle")) {
                notEqual('status', UberWorkerMeta.Status.IDLE)
            }
            if (!params.getBoolean("includeStopped")) {
                notEqual('status', UberWorkerMeta.Status.STOPPED)
            }
        }
        renderResponse([list: list, total: list.totalCount])
    }

    def get() {
        withDomainObject(UberWorkerMeta) { UberWorkerMeta uberWorkerMeta ->
            renderResponse([worker: uberWorkerMeta])
        }
    }

    def create() {
        def json = request.JSON
        if (!json.queues) {
            renderBadRequest([error: [queues: 'NULLABLE']])
            return
        } else if (!json.poolName) {
            renderBadRequest([error: [poolName: 'NULLABLE']])
            return
        }
        def queues = UberQueue.findAllByIdInList(json.queues)
        if (!queues) {
            renderBadRequest([error: [queues: 'INVALID']])
            return
        }

        def maxIndex = UberWorkerMeta.createCriteria().get {
            eq('hostname', uberJobsWorkerService.hostName)
            eq('poolName', json.poolName)
            projections {
                max('index')
            }
        }
        int newIndex = maxIndex != null ? maxIndex + 1 : 0
        def workerMeta = uberJobsWorkerService.startWorker(json.poolName, newIndex, queues)

        renderResponse([worker: workerMeta])
    }

    def pause() {
        withDomainObject(UberWorkerMeta) { UberWorkerMeta uberWorkerMeta ->
            def signal = uberJobsWorkerService.pauseWorker(uberWorkerMeta)
            renderResponse([signal: signal])
        }
    }

    def resume() {
        withDomainObject(UberWorkerMeta) { UberWorkerMeta uberWorkerMeta ->
            def signal = uberJobsWorkerService.resumeWorker(uberWorkerMeta)
            renderResponse([signal: signal])
        }
    }

    def stop() {
        withDomainObject(UberWorkerMeta) { UberWorkerMeta uberWorkerMeta ->
            def signal = uberJobsWorkerService.stopWorker(uberWorkerMeta)
            renderResponse([signal: signal])
        }
    }

}
