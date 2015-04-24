package grails.plugin.uberjobs

class UberJobFailure {

    /**
     * The exception class
     */
    String exception

    /**
     * The exception message
     */
    String message

    /**
     * complete stacktrace of the exception
     */
    String stacktrace

    static mapping = {
        stacktrace type: 'text'
    }

    static belongsTo = [job: UberJob]

    static constraints = {
    }
}
