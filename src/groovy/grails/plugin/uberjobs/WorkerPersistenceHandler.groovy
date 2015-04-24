package grails.plugin.uberjobs

import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor

@Log4j
class WorkerPersistenceHandler {

    PersistenceContextInterceptor persistenceInterceptor

    WorkerPersistenceHandler(PersistenceContextInterceptor persistenceInterceptor) {
        this.persistenceInterceptor = persistenceInterceptor
    }

    void bindSession() {
        if (persistenceInterceptor == null)
            throw new IllegalStateException("No persistenceInterceptor property provided")

        persistenceInterceptor.init()
    }

    void unbindSession() {
        if (persistenceInterceptor == null)
            throw new IllegalStateException("No persistenceInterceptor property provided")

        try {
            persistenceInterceptor.flush()
        } catch (Exception exception) {
            fireThreadException(exception)
        } finally {
            persistenceInterceptor.destroy()
        }
    }

    private static void fireThreadException(final Exception exception) {
        Thread thread = Thread.currentThread()
        if (thread.uncaughtExceptionHandler == null) {
            //Logging the problem that the current thread doesn't have an uncaught exception handler set.
            //Bare throwing an exception might not have any effect in such a case.
            String message = "No handler property provided for the current background worker thread ${thread.name} when trying to handle an exception."
            log.error(message, exception)
        } else {
            thread.uncaughtExceptionHandler.uncaughtException(thread, exception)
        }
    }

}
