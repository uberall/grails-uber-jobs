package grails.plugin.uberjobs

import grails.transaction.Transactional
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.WebApplicationContext

import javax.servlet.ServletContext

@Transactional
class UberJobsWorkerService extends AbstractUberJobsService {

    def uberJobsQueueService
    def uberJobsWorkerMetaService

    private static final List WORKERS = []

    def createWorkersFromConfig() {
        def currentCount = UberWorkerMeta.countByHostname(hostName)
        if (currentCount) {
            log.info("pruning $currentCount workers from database")

            // deleting children from many-to-many seems to be not that easy ... http://dante.cassanego.net/?p=147
            UberWorkerMeta.findAllByHostname(hostName).each { w ->
                def tmp = []
                w.queues.each { tmp << it }
                tmp.each { w.removeFromQueues(it) }
                w.delete()
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
                queues << uberJobsQueueService.findOrCreate(name.toString())
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
            workerMeta = uberJobsWorkerMetaService.create(poolName, index, queues)
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
        log.info "Starting worker processing queues: ${queues.name}"

        PollMode pollMode = grailsApplication.config.grails.uberjobs.pollMode
        if (!pollMode) {
            pollMode = PollMode.ROUND_ROBIN
            log.info("no pollMode specified, using $pollMode")
        }

        // use custom worker class if specified
        UberWorker worker
        def customWorkerClass = grailsApplication.config.grails.uberjobs.worker
        if (customWorkerClass && customWorkerClass in UberWorker) {
            log.info("using ${customWorkerClass.class} as worker")
            worker = customWorkerClass.newInstance(queues)
        } else {
            if (customWorkerClass)
                log.warn('The specified custom worker class does not extend UberWorker. Ignoring it')
            worker = new UberWorker(queues, workerMeta, pollMode)
        }

        // add custom job throwable handler if specified
        def customJobThrowableHandler = grailsApplication.config.grails.uberjobs.jobThrowableHandler
        if (customJobThrowableHandler && customJobThrowableHandler in UberJobThrowableHandler) {
            log.info("using ${customJobThrowableHandler.class} as throwable handler")
            worker.jobThrowableHandler = customJobThrowableHandler.newInstance()
        } else if (customJobThrowableHandler) {
            log.warn('The specified job throwable handler class does not implement UberJobThrowableHandler. Ignoring it')
        }

        // add custom listener if specified (not implemented yet)
//        def customListenerClass = grailsApplication.config.grails.uberjobs.listener
//        if (customListenerClass && customListenerClass in UberWorkerListener) {
//            worker.workerEventEmitter.addListener(customListenerClass.newInstance() as WorkerListener)
//        } else if (customListenerClass) {
//            log.warn('The specified custom listener class does not implement UberWorkerListener. Ignoring it')
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
        new Thread(worker, worker.getName()).start()

        worker
    }

    /**
     * Sets the locale to the configured value.
     * Like this, we can resolve messages in jobs.
     */
    private static void addRequestContext() {
        Locale.setDefault(Locale.GERMANY)
        ServletContext servletContext = ServletContextHolder.getServletContext()
        WebApplicationContext ctx = servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as WebApplicationContext
        GrailsWebRequest req = GrailsWebUtil.bindMockWebRequest(ctx)
        // lets set the preferredLocale to the default locale, as we use this to set locales in jobs
        req.currentRequest.addPreferredLocale(Locale.default)
    }

}
