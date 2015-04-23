package grails.plugin.uberjobs

import groovy.util.logging.Log4j
import org.joda.time.DateTime

@Log4j
class UberSchedulingThread extends Thread {

    UberJobsSchedulingService uberSchedulingService
    boolean work = true
    DateTime waitTill = DateTime.now()

    public UberSchedulingThread(UberJobsSchedulingService uberSchedulingService) {
        this.uberSchedulingService = uberSchedulingService
    }

    @Override
    /**
     * Checks every second if the waitTill time is reached, if so executes the doPoll method of the underlying service, if not just sleeps for a second
     * this brings to fancy side effects:
     * a) we can gracefully stop the thread every second
     * b) the waitTill time could be changed from within the service, to make this thread execute the service (e.g. on dynamic trigger creation or execution)
     */
    void run() {
        log.info("Starting")
        while (!isInterrupted()) {
            try {
                if(DateTime.now().isAfter(waitTill)){
                    // the actual work is done in the service, the only thing that we get back is how long we should wait till the next doPoll execution
                    waitTill = uberSchedulingService.doPoll()
                }
                sleep(1000)
            } catch (InterruptedException e) {
                log.info("got $e.message, will stop working")
                work = false
            }
        }
    }

    @Override
    boolean isInterrupted() {
        return !work
    }
}
