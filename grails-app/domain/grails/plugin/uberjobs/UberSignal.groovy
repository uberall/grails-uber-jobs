package grails.plugin.uberjobs

import grails.converters.JSON

/**
 * Class that is used to send signals to workers etc.
 */
class UberSignal {

    /**
     * the object that should receive this signal (e.g. the name of a worker)
     */
    String receiver

    /**
     * a value indicating the signal (e.g. WORKER_PAUSE -> worker should go to pause state)
     */
    Value value

    /**
     * an optional JSON blob that is used for passing arguments
     */
    String argumentsJSON

    /**
     * Helper field to put and receive args, will be serialized to JSON in beforeValidate and load again in afterLoad
     */
    transient List arguments = []

    enum Value {
        /**
         * signal to tell a worker to go to pause state
         */
        WORKER_PAUSE,

        /**
         * signal to tell a worker to continue working
         */
        WORKER_RESUME,

        /**
         * signal to tell a worker to stop working (e.g. shutdownAllWorkers)
         */
        WORKER_STOP,

        /**
         * signal to tell a worker to stop working on a specific queue
         */
        WORKER_QUEUE_REMOVE,

        /**
         * signal to tell a worker to start working on a specific queue
         */
        WORKER_QUEUE_ADD
    }

    void beforeValidate() {
        argumentsJSON = ((arguments) as JSON).toString()
    }

    void afterLoad() {
        arguments = JSON.parse(argumentsJSON) as List
    }

    /**
     * do not store arguments
     */
    static transients = ['arguments']

    static constraints = {
        key: 'argumentsJSON'
    }
}
