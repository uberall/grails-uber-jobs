package grails.plugin.uberjobs

abstract class AbstractUberService {

    def grailsApplication

    def getConfig() {
        grailsApplication.config.grails.uberjobs
    }

    String getHostName() {
        InetAddress.getLocalHost().getHostName()
    }

}
