package grails.plugin.uberjobs

import grails.converters.JSON
import grails.util.GrailsWebUtil
import groovy.util.logging.Log4j
import org.apache.commons.lang.math.RandomUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.joda.time.DateTime
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.web.context.WebApplicationContext

import javax.servlet.ServletContext

/**
 * A worker is a Thread that polls queues and processes jobs by calling a jobs perform method.
 */
@Log4j
class UberWorker implements Runnable {

    protected UberJobThrowableHandler jobThrowableHandler
    protected LinkedList<UberQueue> queueNames = new LinkedList<UberQueue>()
    protected Thread threadRef
    protected UberWorkerMeta workerMeta
    protected PollMode pollMode
    protected Locale locale
    protected boolean paused
    protected WorkerPersistenceHandler persistenceHandler
    protected long emptyQueueSleepTime = 2000 // 2 seconds
    protected long pauseSleepTime = 5000 // 5 seconds

    /**
     * Creates a new UberWorker.
     * The worker will only listen to the supplied queues.
     *
     * @param workerMeta the UberWorkerMeta that holds information about this worker
     * @param pollMode the poll mode this worker should use to poll the queues
     * @throws IllegalArgumentException if queues is null
     */
    public UberWorker(UberWorkerMeta workerMeta, PollMode pollMode) {
        this.workerMeta = workerMeta
        this.pollMode = pollMode
        workerMeta.status = UberWorkerMeta.Status.STARTING
        setQueues(workerMeta.queues)
    }

    /**
     * Sets the queues this worker should grab jobs from.
     *
     * @param queues the queues
     */
    protected void setQueues(Collection<UberQueue> queues) {
        checkQueues(queues)
        queueNames.clear()
        queueNames.addAll(queues)
    }

    /**
     * Verify that the given queues are all valid.
     *
     * @param queues the given queues
     */
    protected static void checkQueues(Iterable<UberQueue> queues) {
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
     * Stop this worker by calling stop() on any thread.
     */
    @Override
    public void run() {
        if (workerMeta.status != UberWorkerMeta.Status.STARTING) {
            throw new IllegalStateException("This UberWorker cannot be started! (status was $workerMeta.status)")
        }

        log.info("spinning up worker thread")

        try {
            threadRef = Thread.currentThread()
            workerMeta.status = UberWorkerMeta.Status.IDLE

            if (locale) {
                setRequestContextLocale(locale)
            }

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
                    throw new IllegalArgumentException("unknown poll mode: $pollMode")
            }
        } finally {
            log.info("worker thread stopped")
            threadRef = null
        }
    }

    /**
     * Polls the queues for jobs in a drain-queue fashion.
     */
    protected void pollDrainQueue() {
        try {
            while (idle) {
                boolean worked = false
                queueNames.each { UberQueue curQueue ->
                    if (!worked) {
                        checkPaused()

                        // Might have been waiting in poll()/checkSignals() for a while
                        if (idle) {
                            UberJob job = pop(curQueue)

                            if (job) {
                                process(job, curQueue)
                                worked = true
                            }
                        }
                    }
                }

                if (!worked) {
                    log.trace("all queues empty, sleeping for $emptyQueueSleepTime")
                    Thread.sleep(emptyQueueSleepTime)
                }
            }
        } catch (InterruptedException ignore) {
            if (!stopped) {
                log.error("worker $name has been interrupted but has not been stopped!")
                setWorkerStatus(UberWorkerMeta.Status.STOPPED)
            }
        } catch (Throwable t) {
            log.error("error in worker $name", t)
            setWorkerStatus(UberWorkerMeta.Status.STOPPED)
        }
    }

    /**
     * Polls the queues for jobs in a round-robin fashion.
     */
    protected void pollRoundRobin() {
        try {
            int missCount = 0
            UberQueue curQueue

            while (idle) {
                curQueue = queueNames.poll()
                queueNames.add(curQueue) // Rotate the queues
                checkPaused()

                // Might have been waiting in poll()/checkSignals() for a while, so check the state again
                if (idle) {
                    UberJob job = pop(curQueue)

                    if (job) {
                        process(job, curQueue)
                        missCount = 0
                    } else if (++missCount >= queueNames.size() && idle) {
                        // Keeps worker from busy-spinning on empty queues
                        missCount = 0
                        log.trace("all queues empty, sleeping for $emptyQueueSleepTime")
                        Thread.sleep(emptyQueueSleepTime)
                    }
                }
            }
        } catch (InterruptedException ignore) {
            if (!stopped) {
                log.error("worker $name has been interrupted but has not been stopped!")
                setWorkerStatus(UberWorkerMeta.Status.STOPPED)
            }
        } catch (Throwable t) {
            log.error("error in worker $name", t)
            setWorkerStatus(UberWorkerMeta.Status.STOPPED)
        }
    }

    protected void pollRandom() {
        // TODO: implement
        throw new UnsupportedOperationException("random poll mode is not yet implemented!")
    }

    /**
     * Stop this Worker.
     * The worker cannot be started again; create a new worker in this case.
     *
     * @param now if true, an effort will be made to stop any job in progress
     */
    void stop(boolean now = false) {
        if (stopped) {
            log.debug("cannot stop already stopped worker")
            return
        }

        setWorkerStatus(UberWorkerMeta.Status.STOPPED)

        if (now) {
            threadRef?.interrupt()
        }
    }

    /**
     * Executes the given job.
     *
     * @param job the Job to process
     * @param curQueue the queue the payload came from
     */
    protected void process(UberJob job, UberQueue curQueue) {
        def started = DateTime.now()

        try {
            log.debug("processing job $job.id, worker is now WORKING")
            setWorkerStatus(UberWorkerMeta.Status.WORKING)

            def instance = job.job.jobBean

            persistenceHandler?.bindSession()
            instance.perform(* job.arguments)
            persistenceHandler?.unbindSession()

            success(job)
        } catch (Throwable t) {
            failure(t, job, curQueue)
        } finally {
            log.debug("processing job $job.id finished, worker is now IDLE")
            setWorkerStatus(UberWorkerMeta.Status.IDLE)
            job.done = DateTime.now()
            job.started = started
            job.save()
        }
    }

    /**
     * Called when we successfully executed a job.
     *
     * @param job the UberJob that succeeded
     */
    protected void success(UberJob job) {
        job.status = UberJob.Status.SUCCESSFUL
        job.save()
    }

    /**
     * Called when the job we executed threw an throwable.
     *
     * @param t the throwable
     * @param job the UberJob that was executed
     * @param curQueue the current queue we worked on
     */
    protected void failure(Throwable t, UberJob job, UberQueue curQueue) {
        jobThrowableHandler?.onThrowable(t, job, curQueue)
        job.status = UberJob.Status.FAILED
        job.save()

        UberJobFailure failure = new UberJobFailure()
        failure.exception = t.class.toString()
        failure.message = t.message
        failure.stacktrace = (t.stackTrace as JSON).toString()
        failure.job = job
        failure.save()
    }

    String getName() {
        return workerMeta.name
    }

    /**
     * Returns true if this worker has been stopped.
     *
     * @return
     */
    boolean isStopped() {
        return workerMeta.status == UberWorkerMeta.Status.STOPPED
    }

    /**
     * Returns true if this worker is currently paused.
     *
     * @return
     */
    boolean isPaused() {
        return workerMeta.status == UberWorkerMeta.Status.PAUSED
    }

    /**
     * Returns true if this worker is currently processing a job (aka WORKING).
     *
     * @return
     */
    boolean isProcessingJob() {
        return workerMeta.status == UberWorkerMeta.Status.WORKING
    }

    /**
     * Returns true if this worker is currently not processing a job (aka IDLE).
     *
     * @return
     */
    boolean isIdle() {
        return workerMeta.status == UberWorkerMeta.Status.IDLE
    }

    /**
     * Toggles the pause state of this worker.
     *
     * @param paused
     */
    void togglePause(boolean paused) {
        this.paused = paused
    }

    Collection<UberQueue> getQueues() {
        return Collections.unmodifiableCollection(this.queueNames)
    }

    /**
     * Pops a job from the given queue.
     * Uses optimistic locking to check if another worked grabbed the job in between and returns null in that case.
     * Otherwise the job - which also has been marked as WORKING - is returned.
     *
     * @param curQueue the queue to pop a job from
     * @return a Job which this worker may work on or null if the queue is empty
     */
    protected UberJob pop(UberQueue queue) {
        UberJob job = null

        log.trace("popping from queue $queue.name")

        try {
            job = UberJob.findByStatusAndQueueAndDoAtLessThan(UberJob.Status.OPEN, queue, DateTime.now())
            if (job && job.job.enabled && job.queue.enabled) {
                job.status = UberJob.Status.WORKING
                job.save(flush: true)
                log.debug("popped job from queue $queue.name")
            }
        } catch (OptimisticLockingFailureException ignore) {
            log.debug("another worker already popped job $job.id, trying queue $queue.name again in a bit")
            threadRef.sleep(RandomUtils.nextInt(1000))
            return pop(queue)
        }

        return job
    }

    /**
     * Checks if this worker is paused. If so, we sleep for some time until we should resume.
     */
    protected void checkPaused() {
        if (paused) {
            setWorkerStatus(UberWorkerMeta.Status.PAUSED)

            while (paused) {
                log.trace("worker is paused, sleeping for $pauseSleepTime")
                threadRef.sleep(pauseSleepTime)
            }

            setWorkerStatus(UberWorkerMeta.Status.IDLE)
            log.debug("worker was released from paused mode")
        }
    }

    protected void setWorkerStatus(UberWorkerMeta.Status status) {
        workerMeta.status = status
        workerMeta.save(flush: true)
    }

    @Override
    public String toString() {
        return getName()
    }

    /**
     * Sets the locale for the current thread to the specified value.
     * Like this, we can resolve messages in jobs.
     */
    private static void setRequestContextLocale(Locale locale) {
        Locale.setDefault(locale)
        ServletContext servletContext = ServletContextHolder.getServletContext()
        WebApplicationContext ctx = servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as WebApplicationContext
        GrailsWebRequest req = GrailsWebUtil.bindMockWebRequest(ctx)
        req.currentRequest.addPreferredLocale(Locale.default)
    }

}
