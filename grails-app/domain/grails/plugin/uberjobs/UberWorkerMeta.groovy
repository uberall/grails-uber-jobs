package grails.plugin.uberjobs

import org.joda.time.DateTime

/**
 * Worker meta information
 */
class UberWorkerMeta {

    /**
     * The queues this worker is allowed to work on
     */
    static hasMany = [queues: UberQueue]

    /**
     * the hostname this worker is working on
     */
    String hostname

    /**
     * the pool that this worker belongs to
     */
    String poolName

    /**
     * the index inside the pool (needed for determining the name)
     */
    int index

    /**
     * the status of this worker
     */
    Status status

    /**
     * create and update times
     */
    DateTime dateCreated, lastUpdated

    static constraints = {
        hostname nullable: false
        poolName nullable: false
        index min: 1
    }

    /**
     * Status information for workers
     */
    enum Status {

        /**
         * Worker is currently starting
         */
        STARTING,

        /**
         * Worker is doing nothing as no work has to be done
         */
        IDLE,

        /**
         * Worker is working
         */
        WORKING,

        /**
         * Worker is paused
         */
        PAUSED,

        /**
         * Worker was killed
         */
        STOPPED
    }
}
