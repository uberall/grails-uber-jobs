package grails.plugin.uberjobs

abstract class AbstractUberJobsService {

    def grailsApplication

    def getConfig() {
        grailsApplication.config.grails.uberjobs
    }

    String getHostName() {
        InetAddress.getLocalHost().getHostName()
    }

}
