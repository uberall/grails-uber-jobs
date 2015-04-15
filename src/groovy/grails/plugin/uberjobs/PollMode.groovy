package grails.plugin.uberjobs

/**
 * The poll mode specifies how queues are drained by a worker.
 */
enum PollMode {

    /**
     * always drain a queue before moving on to the next one
     */
    DRAIN_QUEUE,

    /**
     * immediately move on to the next queue after processing a job
     */
    ROUND_ROBIN,

    /**
     * poll jobs in a completely random fashion
     */
    RANDOM

}
