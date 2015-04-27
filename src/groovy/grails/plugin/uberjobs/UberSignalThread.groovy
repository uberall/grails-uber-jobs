package grails.plugin.uberjobs

import groovy.util.logging.Log4j

@Log4j
class UberSignalThread extends Thread {

    UberJobsSignalService uberSignalService
    boolean work = true
    long pollDelay

    UberSignalThread(UberJobsSignalService uberSignalService, long pollDelay) {
        this.uberSignalService = uberSignalService
        this.pollDelay = pollDelay
    }

    @Override
    public void run() {
        log.info("spinning up signal thread")

        while (!isInterrupted()) {
            try {
                uberSignalService.poll()
                sleep(pollDelay)
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
