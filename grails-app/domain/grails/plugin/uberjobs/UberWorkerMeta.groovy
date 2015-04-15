package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

/**
 * Worker meta information
 */
class UberWorkerMeta implements UberApiResponseObject {

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
        index min: 0
    }

    static mapping = {
        index column: 'pool_index'
        dateCreated type: PersistentDateTime
        lastUpdated type: PersistentDateTime
    }

    /**
     * Represent the current state of a Worker
     */
    enum Status {

        /**
         * Worker is currently starting
         */
        STARTING,

        /**
         * Worker is currently idle
         */
                IDLE,

        /**
         * Worker is currently working
         */
                WORKING,

        /**
         * Worker is currently paused
         */
                PAUSED,

        /**
         * Worker has been stopped
         */
                STOPPED
    }

    @Override
    Map toResponseObject() {
        [
                id         : id,
                hostname   : hostname,
                poolName   : poolName,
                index      : index,
                status     : status.toString(),
                dateCreated: dateCreated?.millis,
                lastUpdated: lastUpdated?.millis,
                queues     : queues.collect { [id: it.id, name: it.name] }
        ]
    }

}
