package grails.plugin.uberjobs

import groovy.util.logging.Log4j
import org.joda.time.DateTime
import org.springframework.dao.OptimisticLockingFailureException

/**
 * A worker is a Thread that polls queues and processes jobs by calling a jobs perform method.
 */
@Log4j
class UberWorker implements Runnable {

    protected UberJobThrowableHandler jobThrowableHandler
    protected LinkedList<UberQueue> queueNames = new LinkedList<UberQueue>()
    protected Thread threadRef
    protected String name
    protected UberWorkerMeta workerMeta
    protected PollMode pollMode
    protected boolean paused = false
    protected static final long EMPTY_QUEUE_SLEEP_TIME = 1000 // 1 second

    /**
     * Creates a new UberWorker.
     * The worker will only listen to the supplied queues.
     *
     * @param queues the list of queues to poll
     * @param workerMeta the UberWorkerMeta that holds information about this worker
     * @param pollMode the poll mode this worker should use to poll the queues
     * @throws IllegalArgumentException if queues is null
     */
    public UberWorker(Collection<UberQueue> queues, UberWorkerMeta workerMeta, PollMode pollMode) {
        setQueues(queues)
        this.workerMeta = workerMeta
        this.pollMode = pollMode
        workerMeta.status = UberWorkerMeta.Status.STARTING
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
     * Stop this worker by calling end() on any thread.
     */
    @Override
    public void run() {
        if (workerMeta.status != UberWorkerMeta.Status.STARTING) {
            throw new IllegalStateException("This UberWorker is already running!")
        }

        log.info("spinning up worker thread")

        try {
            threadRef = Thread.currentThread()
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

                        // Might have been waiting in poll()/checkPaused() for a while
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
                    log.trace("no more work, sleeping ...")
                    Thread.sleep(EMPTY_QUEUE_SLEEP_TIME)
                }
            }
        } catch (InterruptedException ignore) {
            if (!stopped) {
                log.error("worker $name has been interrupted but has not been stopped!")
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
                if (curQueue) {
                    queueNames.add(curQueue) // Rotate the queues
                    checkPaused()

                    // Might have been waiting in poll()/checkPaused() for a while, so check the state again
                    if (idle) {
                        UberJob job = pop(curQueue)

                        if (job) {
                            process(job, curQueue)
                            missCount = 0
                        } else if (++missCount >= queueNames.size() && idle) {
                            // Keeps worker from busy-spinning on empty queues
                            missCount = 0
                            log.trace("no more work, sleeping ...")
                            Thread.sleep(EMPTY_QUEUE_SLEEP_TIME)
                        }
                    }
                }
            }
        } catch (InterruptedException ignore) {
            if (!stopped) {
                log.error("worker $name has been interrupted but has not been stopped!")
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
    void end(final boolean now) {
        setWorkerStatus(UberWorkerMeta.Status.STOPPED)

        if (now) {
            Thread workerThread = threadRef
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
            log.debug("processing job $job.id, worker is now WORKING")
            setWorkerStatus(UberWorkerMeta.Status.WORKING)

            // get an instance of the job bean ...
            def instance = job.job.jobBean

            // ... and execute it
            instance.perform(* job.arguments)
            success(job)
        } catch (Throwable t) {
            failure(t, job, curQueue)
        } finally {
            log.debug("processing job $job.id finished, worker is now IDLE")
            setWorkerStatus(UberWorkerMeta.Status.IDLE)
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
        return paused
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
            if (job) {
                job.status = UberJob.Status.WORKING
                job.save()
                log.debug("popped job from queue $queue.name")
            }
        } catch (OptimisticLockingFailureException ignore) {
            log.debug("another worker already popped job $job.id, trying queue $queue.name again ...")
            return pop(queue)
        }

        return job
    }

    /**
     * Checks to see if worker is paused. If so, wait until unpaused.
     */
    protected void checkPaused() {
        if (paused) {
            while (paused) {
                try {
                    log.debug("now waiting in checkPaused")
                    paused.wait()
                } catch (InterruptedException ie) {
                    log.warn("Worker interrupted in checkPaused", ie)
                }
            }
        }
    }

    protected void setWorkerStatus(UberWorkerMeta.Status status) {
        workerMeta.status = status
        workerMeta.save()
    }

    @Override
    public String toString() {
        return getName()
    }

}
