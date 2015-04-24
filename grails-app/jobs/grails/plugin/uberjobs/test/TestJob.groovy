package grails.plugin.uberjobs.test

import org.apache.commons.lang.math.RandomUtils

class TestJob {

    def perform(long waitTime = RandomUtils.nextInt(5000)) {
        sleep(waitTime)
    }
}