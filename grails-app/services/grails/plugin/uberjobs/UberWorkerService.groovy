package grails.plugin.uberjobs

import grails.transaction.Transactional

@Transactional
class UberWorkerService extends AbstractUberService {

    def uberQueueService
    def uberWorkerMetaService

    private static final List WORKERS = []

    def createWorkersFromConfig() {
        def currentCount = UberWorkerMeta.countByHostname(hostName)
        if(currentCount) {
            log.info("pruning $currentCount workers from database")
            UberWorkerMeta.findAllByHostname(hostName).each {
                it.delete()
            }
        }
        // iterate over workers configurations
        config.workers.each { String poolName, config ->
            if (poolName in ['update', 'cleanup', 'restart']) {
                return
            }
            List queueNames
            // check queue configuration, should be a list, a single string or a closure that is returning a list
            if (config.queueNames in List) {
                queueNames = config.queueNames
            } else if (config.queueNames instanceof Closure) {
                def closureResult = config.queueNames.call()
                assert closureResult in List, "$poolName's queueNames closure has not returned a list!"
                queueNames = closureResult
            } else if (config.queueNames in String) {
                queueNames = [config.queueNames]
            } else {
                throw new RuntimeException("queueNames of $poolName must be a List, Closure (returning a list) or a String. ${config.queueNames.class.simpleName} is not allowed!")
            }
            def queues = []
            queueNames.each { name ->
                queues << uberQueueService.findOrCreate(name.toString())
            }
            // start the appropiate amount of workers
            config.workers.times { int index ->
                start(poolName, index, queues)
            }
        }
    }

    def shutdown(){
        log.info("Shutting down ${UberWorkerMeta.countByHostnameAndStatusNotEqual(hostName, UberWorkerMeta.Status.STOPPED)}")
        WORKERS.each { worker ->
            //TODO: it.end()
        }
        boolean allDone = false
        int attempts = 10
        while(!allDone){
            sleep(1000)
            int notStopped = UberWorkerMeta.countByHostnameAndStatusNotEqual(hostName, UberWorkerMeta.Status.STOPPED)
            log.info("Shutdown attempt ${attempts-9} of $attempts -> $notStopped Workers has not beed stopped by now.")
            allDone = notStopped == 0 || attempts == 1
            attempts--
        }
    }

    UberWorker start(String poolName, int index, List<UberQueue> queues) {
        UberWorkerMeta workerMeta = UberWorkerMeta.findByPoolNameAndHostnameAndIndex(poolName, hostName, index)
        if (!workerMeta) {
            workerMeta = uberWorkerMetaService.create(poolName, index, queues)
        } else if (config.workers.update) {
            // TODO: UPDATE
        }

        startWorker(queues, workerMeta)
    }

    /**
     * Starts a worker that will poll the specified queues for jobs to process.
     *
     * @param queues the queues this worker should poll for jobs to process
     * @return the started worker
     */
    UberWorker startWorker(List<UberQueue> queues, UberWorkerMeta workerMeta) {
        log.info "Starting worker processing queues: ${queues}"

        PollMode pollMode = grailsApplication.config.grails.uberjobs.pollMode
        if (!pollMode) {
            pollMode = PollMode.ROUND_ROBIN
            log.info("no pollMode specified, using $pollMode")
        }

        // use custom worker class if specified
        UberWorker worker
        def customWorkerClass = grailsApplication.config.grails.uberjobs.custom.worker.clazz
        if (customWorkerClass && customWorkerClass in UberWorker) {
            worker = customWorkerClass.newInstance(queues)
        } else {
            if (customWorkerClass)
                log.warn('The specified custom worker class does not extend UberWorker. Ignoring it')
            worker = new UberWorker(queues, workerMeta, grailsApplication, pollMode)
        }

        // add custom listener if specified (not implemented yet)
//        def customListenerClass = grailsApplication.config.grails.uberjobs.custom.listener.clazz
//        if (customListenerClass && customListenerClass in UberWorkerListener) {
//            worker.workerEventEmitter.addListener(customListenerClass.newInstance() as WorkerListener)
//        } else if (customListenerClass) {
//            log.warn('The specified custom listener class does not implement UberWorkerListener. Ignoring it')
//        }

        // add custom job throwable handler if specified
        def customJobThrowableHandler = grailsApplication.config.grails.uberjobs.custom.jobThrowableHandler.clazz
        if (customJobThrowableHandler && customJobThrowableHandler in JobThrowableHandler) {
            worker.jobThrowableHandler = customJobThrowableHandler.newInstance() as JobThrowableHandler
        } else if (customJobThrowableHandler) {
            log.warn('The specified custom job throwable handler class does not implement JobThrowableHandler. Ignoring it')
        }

        // skip persistence if specified (not yet implemented - currently all workers support persistence)
//        if (!grailsApplication.config.grails.uberjobs.skipPersistence) {
//            log.debug("Enabling Persistence for all Jobs")
//            def autoFlush = grailsApplication.config.grails.uberjobs.autoFlush ?: true
//            def workerPersistenceListener = new WorkerPersistenceListener(persistenceInterceptor, autoFlush)
//            worker.workerEventEmitter.addListener(workerPersistenceListener, WorkerEvent.JOB_EXECUTE, WorkerEvent.JOB_SUCCESS, WorkerEvent.JOB_FAILURE)
//        }

        // enable monitoring if specified (not yet implemented)
//        boolean monitoring = grailsApplication.config.grails.uberjobs.monitoring as boolean
//        if (monitoring) {
//            log.debug("Enabling Monitoring for all Jobs")
//            def workerMonitorListener = new WorkerMonitorListener(this)
//            worker.workerEventEmitter.addListener(workerMonitorListener, WorkerEvent.JOB_EXECUTE, WorkerEvent.JOB_SUCCESS, WorkerEvent.JOB_FAILURE)
//        }

        // add the worker to the list of known workers
        WORKERS.add(worker)

        // start the actual worker thread
        String workerName = worker.getName()
        Thread workerThread = new Thread(worker, workerName)
        workerThread.start()

        worker
    }

}
