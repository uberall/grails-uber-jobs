package grails.plugin.uberjobs.test

class ScheduledTestJob {

    static queueName = 'scheduledTestQueue'
    static singletonJob = true
    static triggers = {
//        cron name: 'scheduledTestOne',
//                cronExpression: "0 0 22 * * ?",
//                args: [100]
//
//        cron name: 'scheduledTestTwo',
//                queueName: 'test-queue',
//                cronExpression: "0 0 7 * * ?",
//                args: [5000]
        cron name: 'scheduledTestThree',
                queueName: 'test-queue',
                cronExpression: "*/30 * * * * ?",
                args: [5000]
    }

    def perform(long waitTime = 1000) {
        log.info("zzzZZZzzzzz")
        sleep(waitTime)
    }
}