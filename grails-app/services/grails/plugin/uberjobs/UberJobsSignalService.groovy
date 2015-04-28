package grails.plugin.uberjobs

import grails.transaction.Transactional
import org.springframework.beans.factory.InitializingBean

@Transactional
class UberJobsSignalService extends AbstractUberJobsService implements InitializingBean {

    /**
     * the actual thread in which all the work is done
     */
    private static UberSignalThread signalThread

    def uberJobsWorkerService

    /**
     * Start the scheduling thread of VM
     * IllegalThreadStateException is thrown if thread was already started
     * @return
     */
    def startThread() {
        log.info("Starting signal thread with poll delay $pollDelay")
        signalThread.setName("$hostName#UberSignalThread")
        signalThread.start()
    }

    def stopThread() {
        if (signalThread != null) {
            log.info("Stopping UberSignalThread")
            signalThread.interrupt()
        }
    }

    def handleSignal(UberSignal signal) {
        log.debug("handling signal: $signal.value (args: $signal.arguments)")

        switch (signal.value) {
            case UberSignal.Value.WORKER_PAUSE:
                emitPauseSignal(signal)
                break
            case UberSignal.Value.WORKER_RESUME:
                emitResumeSignal(signal)
                break
            case UberSignal.Value.WORKER_STOP:
                emitStopSignal(signal)
                break
            case UberSignal.Value.WORKER_START:
                emitStartWorkerSignal(signal)
                break
            default:
                throw new IllegalArgumentException("signal $signal.id has no value!")
        }
    }

    /**
     * Polls the signal queue.
     */
    void poll() {
        UberSignal signal = null

        UberSignal.withNewTransaction {
            signal = UberSignal.findByReceiver(hostName)
            signal?.delete()
        }

        if (!signal) return

        try {
            handleSignal(signal)
        } catch (Throwable t) {
            log.error("error while handling signal", t)
        }
    }

    /**
     * Emits a WORKER_PAUSE signal to a worker.
     *
     * @param pauseSignal the signal that was received
     */
    void emitPauseSignal(UberSignal pauseSignal) {
        String workerName = getWorkerReceiverName(pauseSignal)
        log.debug("emitting pause signal to worker $workerName")
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)
        worker.togglePause(true)
    }

    /**
     * Emits a WORKER_RESUME signal to a worker.
     *
     * @param resumeSignal the signal that was received
     */
    void emitResumeSignal(UberSignal resumeSignal) {
        String workerName = getWorkerReceiverName(resumeSignal)
        log.debug("emitting resume signal to worker $workerName")
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)
        worker.togglePause(false)
    }

    /**
     * Emits a WORKER_STOP signal to a worker.
     *
     * @param stopSignal the signal that was received
     */
    void emitStopSignal(UberSignal stopSignal) {
        String workerName = getWorkerReceiverName(stopSignal)
        log.debug("emitting stop signal to worker $workerName")
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)
        worker.stop(true)
    }

    /**
     * Emits a WORKER_START signal to a worker.
     *
     * @param stopSignal the signal that was received
     */
    void emitStartWorkerSignal(UberSignal startWorkerSignal) {
        UberWorkerMeta meta = UberWorkerMeta.get(startWorkerSignal.arguments.workerMetaId as long)
        log.debug("emitting start worker signal $meta.name")
        uberJobsWorkerService.doStartWorker(meta)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        signalThread = new UberSignalThread(this, pollDelay)
    }

    private int getPollDelay() {
        config.signal.pollDelay ?: 1000
    }

    private static String getWorkerReceiverName(UberSignal signal) {
        String receiver = signal.receiver
        String pool = signal.arguments.pool
        String index = signal.arguments.index
        "$receiver#$pool#$index"
    }

}
