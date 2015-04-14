package grails.plugin.uberjobs

class TestJob {

    def perform(long waitTime = 1000) {
        sleep(waitTime)
    }
}