package grails.plugin.uberjobs.test

class TestJob {

    def perform(long waitTime = 1000) {
        sleep(waitTime)
    }
}