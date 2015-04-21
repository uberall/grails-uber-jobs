package grails.plugin.uberjobs

import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.joda.time.DateTime
import org.springframework.dao.OptimisticLockingFailureException

import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A worker is a Thread that polls queues and processes jobs by calling a jobs perform method.
 */
@Log4j
class UberWorker implements Runnable {

    private GrailsApplication grailsApplication
    protected JobThrowableHandler jobThrowableHandler
    protected BlockingDeque<UberQueue> queueNames = new LinkedBlockingDeque<UberQueue>()
    private AtomicReference<Thread> threadRef = new AtomicReference<Thread>(null)
    private String name
    private final AtomicBoolean paused = new AtomicBoolean(false)
    private UberWorkerMeta workerMeta
    private PollMode pollMode
    protected static final long EMPTY_QUEUE_SLEEP_TIME = 1000 // 1 second

    /**
     * Polls the queues for jobs in a drain-queue fashion and executes them.
     */
    protected void pollDrainQueue() {
        while (workerMeta.status == UberWorkerMeta.Status.IDLE) {
            boolean worked = false

            try {
                queueNames.each { UberQueue curQueue ->
                    if (!worked) {
                        checkPaused()

                        // Might have been waiting in poll()/checkPaused() for a while
                        if (workerMeta.status == UberWorkerMeta.Status.IDLE) {
                            UberJob job = pop(curQueue)

                            if (job != null) {
                                process(job, curQueue)
                                worked = true
                            }
                        }
                    }
                }

                if (!worked) {
//                    log.debug("no more work ... sleeping")
                    Thread.sleep(EMPTY_QUEUE_SLEEP_TIME)
                }
            } catch (InterruptedException ignore) {
                if (!isShutdown()) {
                    log.error("worker $name has been interrupted but has not been shutdown!")
                }
            } catch (Throwable t) {
                log.error("error in worker $name", t)
            }
        }
    }

    /**
     * Polls the queues for jobs in a round-robin fashion and executes them.
     */
    protected void pollRoundRobin() {
        int missCount = 0
        UberQueue curQueue

        while (workerMeta.status == UberWorkerMeta.Status.IDLE) {
            try {
                curQueue = queueNames.poll(EMPTY_QUEUE_SLEEP_TIME, TimeUnit.MILLISECONDS)
                if (curQueue != null) {
                    queueNames.add(curQueue) // Rotate the queues
                    checkPaused()

                    // Might have been waiting in poll()/checkPaused() for a while, so check the state again
                    if (workerMeta.status == UberWorkerMeta.Status.IDLE) {
                        UberJob job = pop(curQueue)

                        if (job != null) {
                            process(job, curQueue)
                            missCount = 0
                        } else if (++missCount >= queueNames.size() && workerMeta.status == UberWorkerMeta.Status.IDLE) {
                            // Keeps worker from busy-spinning on empty queues
                            missCount = 0
//                            log.debug("no more work ... sleeping")
                            Thread.sleep(EMPTY_QUEUE_SLEEP_TIME)
                        }
                    }
                }
            } catch (InterruptedException ignore) {
                if (!isShutdown()) {
                    log.error("worker $name has been interrupted but has not been shutdown!")
                }
            } catch (Throwable t) {
                log.error("error in worker $name", t)
            }
        }
    }

    protected void pollRandom() {}

    /**
     * Creates a new UberWorker.
     * The worker will only listen to the supplied queues.
     *
     * @param queues the list of queues to poll
     * @param workerMeta the UberWorkerMeta that holds information about this worker
     * @param grailsApplication the grailsApplication (to get references to beans)
     * @throws IllegalArgumentException if queues is null
     */
    public UberWorker(Collection<UberQueue> queues, UberWorkerMeta workerMeta, GrailsApplication grailsApplication, PollMode pollMode) {
        setQueues(queues)
        this.workerMeta = workerMeta
        this.grailsApplication = grailsApplication
        this.pollMode = pollMode
        workerMeta.status = UberWorkerMeta.Status.STARTING
    }

    /**
     * Verify that the given queues are all valid.
     *
     * @param queues the given queues
     */
    protected static void checkQueues(final Iterable<UberQueue> queues) {
        if (queues == null) {
            throw new IllegalArgumentException("queues must not be null")
        }

        for (final UberQueue queue : queues) {
            if (queue == null || "".equals(queue)) {
                throw new IllegalArgumentException("queues' members must not be null: " + queues)
            }
        }
    }

    /**
     * Starts this worker.
     * Handles the state transition from STARTING to IDLE and starts polling the queues for jobs.
     * Stop this worker by calling end() on any thread.
     */
    @Override
    public void run() {
        if (workerMeta.status != UberWorkerMeta.Status.STARTING) {
            throw new IllegalStateException("This UberWorker is already running!")
        }

        log.info("spinning up worker thread ...")

        try {
            this.threadRef.set(Thread.currentThread())
            workerMeta.status = UberWorkerMeta.Status.IDLE

            // start polling the queues
            switch (pollMode) {
                case PollMode.DRAIN_QUEUE:
                    pollDrainQueue()
                    break
                case PollMode.ROUND_ROBIN:
                    pollRoundRobin()
                    break
                case PollMode.RANDOM:
                    pollRandom()
                    break
                default:
                    // should not happen, as we check the poll mode in the constructor
                    throw new IllegalArgumentException("unknown poll mode: $pollMode")
            }
        } finally {
            log.info("worker thread stopped ...")
            this.threadRef.set(null)
        }
    }

    /**
     * Shutdown this Worker.<br>
     * <b>The worker cannot be started again; create a new worker in this
     * case.</b>
     *
     * @param now if true, an effort will be made to stop any job in progress
     */
    void end(final boolean now) {
        workerMeta.status = UberWorkerMeta.Status.STOPPED

        if (now) {
            workerMeta.status = UberWorkerMeta.Status.STOPPED
            final Thread workerThread = this.threadRef.get()
            if (workerThread != null) {
                workerThread.interrupt()
            }
        }

        togglePause(false) // Release any threads waiting in checkPaused() - do we need that?
    }

    /**
     * Executes the given job.
     *
     * @param job the Job to process
     * @param curQueue the queue the payload came from
     */
    protected void process(UberJob job, UberQueue curQueue) {
        try {
            log.debug("processing job $job, setting state to WORKING")
            workerMeta.status = UberWorkerMeta.Status.WORKING
            workerMeta.save()

            // get an instance of the job bean ...
            def instance = job.job.jobBean
            // ... and execute it
            execute(job, instance, job.arguments)
        } catch (Throwable t) {
            failure(t, job, curQueue)
        } finally {
            workerMeta.status = UberWorkerMeta.Status.IDLE
            workerMeta.save()
        }
    }

    /**
     * Executes the given queue item.
     *
     * @param job the Job to execute
     * @param instance the job bean
     * @param args the arguments to pass to the jobs perform method
     */
    protected void execute(UberJob job, Object instance, List args) {
        instance.perform(* args)
        success(job)
    }

    /**
     * Called when we successfully executed a job.
     *
     * @param job the Job that succeeded
     */
    protected void success(UberJob job) {
        log.debug("job $job was successful! setting status to SUCCESSFUL")
        job.status = UberJob.Status.SUCCESSFUL
        job.save()
    }

    /**
     * Called when the job we execute throws an exception.
     *
     * @param ex the exception
     * @param job the job that was executed
     * @param curQueue the current queue
     */
    protected void failure(Throwable t, UberJob job, UberQueue curQueue) {
        log.debug("job $job failed! setting status to FAILED and calling throwable handler", t)
        jobThrowableHandler?.onThrowable(t, job, curQueue)
        job.status = UberJob.Status.FAILED
        job.save()
    }

    /**
     * Returns the name of this worker.
     * The name is composed of the hostname, the poolName and the index in that pool separated by a hash (#).
     *
     * @return the name of this worker
     */
    String getName() {
        return "$workerMeta.hostname#$workerMeta.poolName#$workerMeta.index"
    }

    /**
     * Returns true if this worker has been shutdown.
     *
     * @return
     */
    boolean isShutdown() {
        return workerMeta.status == UberWorkerMeta.Status.STOPPED
    }

    /**
     * Returns true if this worker is currently paused.
     *
     * @return
     */
    boolean isPaused() {
        return paused.get()
    }

    /**
     * Returns true if this worker is currently processing a job.
     *
     * @return
     */
    boolean isProcessingJob() {
        return workerMeta.status == UberWorkerMeta.Status.WORKING
    }

    void togglePause(boolean paused) {
        if (paused) {
            // we should go to pause state
        }
        if (workerMeta.status == UberWorkerMeta.Status.PAUSED) return
        this.paused.set(paused)
        synchronized (this.paused) {
            this.paused.notifyAll()
        }
    }

    Collection<UberQueue> getQueues() {
        return Collections.unmodifiableCollection(this.queueNames)
    }

    protected void setQueues(Collection<UberQueue> queues) {
        checkQueues(queues)
        queueNames.clear()
        queueNames.addAll(queues)
    }

//    public void join(final long millis) throws InterruptedException {
//        final Thread workerThread = this.threadRef.get()
//        if (workerThread != null && workerThread.isAlive()) {
//            workerThread.join(millis)
//        }
//    }

    /**
     * Removes a job from the given queue.
     *
     * @param curQueue the queue to remove a job from
     * @return a JSON string of a job or null if there was nothing to de-queue
     */
    protected UberJob pop(UberQueue queue) {
        UberJob job = null

        try {
            job = UberJob.findByStatusAndQueueAndDoAtLessThan(UberJob.Status.OPEN, queue, DateTime.now())
            if (job) {
                job.status = UberJob.Status.WORKING
                job.save()
            }
        } catch (OptimisticLockingFailureException ignore) {
            log.debug("another worker already popped job $job ...")
            job = null
        }

        return job
    }

    /**
     * Checks to see if worker is paused. If so, wait until unpaused.
     */
    protected void checkPaused() {
        if (this.paused.get()) {
            synchronized (this.paused) {
                while (this.paused.get()) {
                    try {
                        log.debug("now waiting in checkPaused")
                        this.paused.wait()
                    } catch (InterruptedException ie) {
                        log.warn("Worker interrupted in checkPaused", ie)
                    }
                }
            }
        }
    }

    /**
     * Returns an instance of the actual job class.
     *
     * @param jobMeta the jobMeta
     * @return instance of the job
     */
    protected def materializeJob(UberJobMeta job) {
        return grailsApplication.mainContext.getBean(job.job)
    }

    @Override
    public String toString() {
        return getName()
    }

}
