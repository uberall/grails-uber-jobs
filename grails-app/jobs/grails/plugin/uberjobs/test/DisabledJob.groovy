package grails.plugin.uberjobs.test

class DisabledJob {

    def perform() {
        throw new RuntimeException("this job is disabled!")
    }
}