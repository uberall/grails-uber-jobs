package grails.plugin.uberjobs

import grails.converters.JSON
import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

/**
 * A item of a Queue which knows what to perform when
 */
class UberJob implements UberApiResponseObject {

    /**
     * The triggers that belong to this job
     */
    static hasMany = [triggers: UberTriggerMeta]

    /**
     * The job failure
     */
    static hasOne = [failure: UberJobFailure]

    /**
     * Every item belongs into a Queue
     */
    static belongsTo = [queue: UberQueue]

    /**
     * do not store arguments
     */
    static transients = ['arguments']

    /**
     * The Job that has to be performed
     */
    UberJobMeta job

    /**
     * The arguments that will be passed into perform
     */
    String argumentsJSON

    /**
     * The jobs status
     */
    Status status

    /**
     * When to work on it
     */
    DateTime doAt

    /**
     * When the worker started working
     */
    DateTime started

    /**
     * When the worker was done working
     */
    DateTime done

    /**
     * When was the item created
     */
    DateTime dateCreated

    /**
     * Helper field to put and receive args, will be serialized to JSON in beforeValidate and load again in afterLoad
     */
    transient List arguments = []

    void beforeValidate() {
        argumentsJSON = ((arguments) as JSON).toString()
    }

    void afterLoad() {
        arguments = JSON.parse(argumentsJSON) as List
    }

    static constraints = {
        job nullable: false
        queue nullable: false
        argumentsJSON nullable: false
        status nullable: false
        doAt nullable: false
        started nullable: true
        done nullable: true
        failure nullable: true
    }

    static mapping = {
        doAt type: PersistentDateTime
        dateCreated type: PersistentDateTime
        started type: PersistentDateTime
        done type: PersistentDateTime
        argumentsJSON column: 'arguments', type: 'text'
    }

    @Override
    Map toResponseObject() {
        [
                id         : id,
                queue      : queueId,
                job        : jobId,
                arguments  : getArguments(),
                status     : status.toString(),
                doAt       : doAt,
                started    : started,
                done       : done,
                failure    : failure?.id,
                dateCreated: dateCreated,
        ]
    }

    enum Status {
        /**
         * Open job, which will be performed as soon as doAt is reached and Workers are free to work on it
         */
        OPEN,

        /**
         * A Worker is currently working on this Job
         */
                WORKING,

        /**
         * This Job was successfully performed
         */
                SUCCESSFUL,

        /**
         * This Job has failed to perform successful
         */
                FAILED,

        /**
         * This Job was marked as to be skipped, so it will not yet be performed
         */
                SKIPPED
    }
}