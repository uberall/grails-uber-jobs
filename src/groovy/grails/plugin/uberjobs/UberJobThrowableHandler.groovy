package grails.plugin.uberjobs

/**
 * Handler for errors thrown by a jobs perform() method.
 */
interface UberJobThrowableHandler {

    /**
     * Called when a jobs perform method throws an Throwable
     *
     * @param throwable the throwable thrown
     * @param job the job where the throwable was thrown
     * @param curQueue the queue
     */
    void onThrowable(Throwable throwable, UberJob job, UberQueue curQueue)

}
