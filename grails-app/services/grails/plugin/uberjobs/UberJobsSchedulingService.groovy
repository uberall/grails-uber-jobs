package grails.plugin.uberjobs

import grails.transaction.Transactional
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.dao.OptimisticLockingFailureException

@Transactional
class UberJobsSchedulingService extends AbstractUberJobsService implements InitializingBean {

    private static UberSchedulingThread schedulingThread
    def uberJobsJobService

    def startThread() {
        if (config.scheduling.thread.name) {
            schedulingThread.setName(config.scheduling.thread.name)
        } else {
            schedulingThread.setName("$hostName#UberSchedulingThread")
        }
        schedulingThread.start()
    }

    def stopThread() {
        if (schedulingThread != null) {
            log.info("Stopping UberSchedulingThread")
            schedulingThread.interrupt()
        }
    }

    /**
     * can be called to force a specific time at which the next poll should happen
     * is called when TriggerMeta is created or updated
     * @param time
     * @return
     */
    def updateThreadsNextExecution(DateTime time, boolean forceIfLater = true) {
        if (schedulingThread && (forceIfLater || time.isBefore(schedulingThread.waitTill))) {
            log.debug("Manually setting next execution time to $time")
            schedulingThread.waitTill = time
        }
    }

    DateTime doPoll() {
        DateTime nearestNext = null
        UberTriggerMeta.where { enabled }.list().each { UberTriggerMeta uberTriggerMeta ->
            if (!uberTriggerMeta.estimatedNextExecution || uberTriggerMeta.estimatedNextExecution.isBefore(DateTime.now())) {
                doExecute(uberTriggerMeta)
            }
            CronExpression cronExpression = new CronExpression(uberTriggerMeta.cronExpression)
            DateTime next = cronExpression.getNextValidTimeAfter(DateTime.now())
            if (!nearestNext || next.isBefore(nearestNext))
                nearestNext = next
        }
        // in the rare case of no triggers are defined at all, we wait can safely tell this thread to wait for an hour
        // if someone adds a trigger at runtime, this service will take care of telling the thread
        if (!nearestNext) {
            nearestNext = DateTime.now().plusHours(1)
        }
        // subtract 0-20 seconds from the nearest next execution and return the result if it is in the future, if not return NOW() + 10sec
        log.info("Next execution time will be $nearestNext ")
        nearestNext
    }

    /**
     * return the next calculated next execution time of the given trigger
     * @param trigger
     * @return
     */
    private void doExecute(UberTriggerMeta trigger) {
        try {
            // set the lastFired date to ensure that two threads are not handling this at the same time
            trigger.lastFired = DateTime.now()
            trigger.save(flush: true)
            log.debug("enqueing $trigger.name")
            uberJobsJobService.enqueue(trigger.job.job, trigger.arguments, trigger.queueName)
        } catch (OptimisticLockingFailureException e) {
            log.info("it looks like another thread already handled $trigger.name, will do nothing")
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        schedulingThread = new UberSchedulingThread(this)
    }
}
