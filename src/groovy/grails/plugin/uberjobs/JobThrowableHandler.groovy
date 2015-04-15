package grails.plugin.uberjobs

/**
 * Handler for errors thrown by a jobs perform() method.
 */
interface JobThrowableHandler {

    def onThrowable(Throwable throwable, UberJob job, UberQueue curQueue)

}
