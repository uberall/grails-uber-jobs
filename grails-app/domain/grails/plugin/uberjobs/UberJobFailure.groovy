package grails.plugin.uberjobs

class UberJobFailure {

    String exception
    String message
    String stacktrace

    static mapping = {
        stacktrace type: 'text'
    }

    static belongsTo = [job: UberJob]

    static constraints = {
    }
}
