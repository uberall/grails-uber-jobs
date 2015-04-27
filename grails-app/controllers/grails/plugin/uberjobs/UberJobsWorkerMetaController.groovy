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
        def poolName = json.poolName
        if (!json.queues) {
            renderBadRequest([error: [queues: 'NULLABLE']])
            return
        } else if (!poolName) {
            poolName = config.workers.manualPoolName ?: "manualPool"
        }
        def queues = null
        def queueIds = json.queues?.collect {it.id as long}
        if(queueIds) {
            queues = UberQueue.findAllByIdInList(queueIds)
        }
        if (!queues) {
            renderBadRequest([error: [queues: 'INVALID']])
            return
        }

        def maxIndex = UberWorkerMeta.createCriteria().get {
            eq('hostname', uberJobsWorkerService.hostName)
            eq('poolName', poolName)
            projections {
                max('index')
            }
        }
        int newIndex = maxIndex != null ? maxIndex + 1 : 0
        def workerMeta = uberJobsWorkerService.createWorkerMeta(poolName, newIndex, queues)
        uberJobsWorkerService.startWorker(workerMeta)

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
