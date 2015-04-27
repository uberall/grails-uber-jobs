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
    def startThread() throws IllegalThreadStateException {
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
            default:
                throw new IllegalArgumentException("signal $signal.id has no value!")
        }
    }

    /**
     * Polls the signal queue.
     */
    void poll() {
        UberSignal signal = UberSignal.findByReceiver(hostName)

        if (signal) {
            log.debug("popped signal: $signal.value (args: $signal.arguments)")
            signal.delete()
            handleSignal(signal)
        }
    }

    /**
     * Emits a WORKER_PAUSE signal to a worker.
     *
     * @param pauseSignal the signal that was received
     */
    void emitPauseSignal(UberSignal pauseSignal) {
        String workerName = getWorkerReceiverName(pauseSignal)
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)

        log.debug("emitting pause signal to worker $workerName")
        worker.togglePause(true)
    }

    /**
     * Emits a WORKER_RESUME signal to a worker.
     *
     * @param resumeSignal the signal that was received
     */
    void emitResumeSignal(UberSignal resumeSignal) {
        String workerName = getWorkerReceiverName(resumeSignal)
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)

        log.debug("emitting resume signal to worker $workerName")
        worker.togglePause(false)
    }

    /**
     * Emits a WORKER_STOP signal to a worker.
     *
     * @param stopSignal the signal that was received
     */
    void emitStopSignal(UberSignal stopSignal) {
        String workerName = getWorkerReceiverName(stopSignal)
        UberWorker worker = uberJobsWorkerService.getWorker(workerName)

        log.debug("emitting resume signal to worker $workerName")
        worker.stop(true)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        signalThread = new UberSignalThread(this, pollDelay)
    }

    private getPollDelay() {
        config.signal.pollDelay ?: 1000
    }

    private static String getWorkerReceiverName(UberSignal signal) {
        String receiver = signal.receiver
        String pool = signal.arguments.pool
        String index = signal.arguments.index
        "$receiver#$pool#$index"
    }

}
