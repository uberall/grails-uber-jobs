import grails.converters.JSON
import grails.converters.XML
import grails.plugin.uberjobs.GrailsUberJobClass
import grails.plugin.uberjobs.UberApiResponseObject
import grails.plugin.uberjobs.UberJobArtefactHandler
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

class UberJobsGrailsPlugin {
    // the plugin version
    def version = "0.1-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.4 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/**",
            "grails-app/jobs/grails/plugin/uberjobs/test/**",
            "test/**"
    ]

    def title = "Uber Jobs Plugin" // Headline display name of the plugin
    def author = "Philipp Eschenbach"
    def authorEmail = "philipp@uberall.com"
    def description = '''\
Brief summary/description of the plugin.
'''
    def dependsOn = ['hibernate4': "4.0 > *"]

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/uber-jobs"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "BSD"

    // Details of company behind the plugin (if there is one)
    def organization = [name: "uberall GmbH", url: "https://uberall.com/"]

    // Any additional developers beyond the author specified above.
    def developers = [[name: "Florian Langenhahn", email: "florian.langenhahn@uberall.com"]]

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "GitHub", url: "https://github.com/uberall/grails-uber-jobs/issues"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/uberall/grails-uber-jobs"]

    def watchedResources = [
            "file:./grails-app/jobs/**/*Job.groovy",
            "file:./plugins/*/grails-app/jobs/**/*Job.groovy"
    ]

    def artefacts = [new UberJobArtefactHandler()]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {

        if (!isPluginEnabled(application)) {
            log.info("uber-jobs is disabled")
            return
        }

        def jobClasses = application.uberJobClasses

        if (!jobClasses) {
            // TODO insert documentationlink to disabling
            log.warn("No UberJobs found, you should probably disable the uber-jobs plugin. see: TODO!")
        } else {

            log.debug("${jobClasses.size()} Jobs found. will start processing them")
            jobClasses.each { GrailsUberJobClass uberJobClass ->
                log.info "parsing information from $uberJobClass"
                configureJobBeans.delegate = delegate
                configureJobBeans(uberJobClass)
            }
        }

        application.domainClasses.each { clazz ->
            def domainClazz = clazz.clazz
            if (domainClazz in UberApiResponseObject) {
                log.info("Registering object marshaller for $domainClazz")
                JSON.registerObjectMarshaller(clazz.clazz) {
                    it.toResponseObject()
                }
                XML.registerObjectMarshaller(clazz.clazz) {
                    it.toResponseObject()
                }
            }
        }

    }

    def configureJobBeans = { GrailsUberJobClass jobClass ->
        def fullName = jobClass.fullName

        "${fullName}Class"(MethodInvokingFactoryBean) {
            targetObject = ref("grailsApplication", true)
            targetMethod = "getArtefact"
            arguments = [UberJobArtefactHandler.TYPE, jobClass.fullName]
        }

        "${fullName}"(ref("${fullName}Class")) { bean ->
            bean.factoryMethod = "newInstance"
            bean.autowire = "byName"
            bean.scope = "prototype"
        }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // AFTER SPRING CONTEXT IS INITIALIZED

        // setup jobs, triggers and queues
        ctx.uberJobsJobService.init()

        // start workers
        ctx.uberJobsWorkerService.createWorkersFromConfig()

        // start scheduling (trigger) thread
        if (ctx.grailsApplication.config.grails.uberjobs.scheduling.thread.active) {
            try {
                ctx.uberJobsSchedulingService.startThread()
            } catch (IllegalThreadStateException e) {
                log.error("Scheduling thread could not be started", e)
            }
        }

        // start signaling thread
        ctx.uberJobsSignalService.startThread()
    }

    def onChange = { event ->
        log.info("$event.source changed")
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onShutdown = { event ->
        def configuration = application.config.grails.uberjobs

        if (configuration.waitForWorkersOnShutdown)
            application.mainContext.uberJobsWorkerService.shutdownAllWorkers()

        if (configuration.scheduling.thread.active)
            application.mainContext.uberJobsSchedulingService.stopThread()

        application.mainContext.uberJobsSignalService.stopThread()
    }

    private static boolean isPluginEnabled(def application) {
        application.config.grails.uberjobs.enabled ?: false
    }
}
