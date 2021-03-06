import grails.plugin.uberjobs.PollMode
import grails.plugin.uberjobs.test.DisabledJob

// configuration for plugin testing - will not be included in the plugin zip

grails.project.groupId = 'grails.plugin.uberjobs'
grails.app.context = '/'
grails {
    uberjobs {
        enabled = true // enable the plugin itself
        waitForWorkersOnShutdown = true // waits for jobs to finish on shutdown
        pollMode = PollMode.ROUND_ROBIN // the poll mode for all workers (see PollMode enum for more information)
        frontend {
            enabled = true
            responseType = 'JSON' // one of JSON or XML, defaults to JSON
            max = 20 // default max list size
            baseUrl = "http://localhost:8080/uberjobs/api" // if you use the uber-jobs-admin plugin, this setting is needed to determine the API url
        }
        signal {
            pollDelay = 1000 // how often should we check for signals (each x milliseconds)
        }
        jobs {
            update = true // enable updating JobMeta on application startup
            cleanup = true // enable pruning JobMeta of Jobs that are not found in code anymore
            requestContextLocale = Locale.GERMANY
            disabled = [DisabledJob]
        }
        triggers {
            update = true // enable updating TriggerMeta on application startup
            cleanup = true // enable pruning TriggerMeta of Triggers that are not found in code anymore
        }
        workers {
            update = true // enable updating of WorkerMeta on application startup
            restart = true // enable starting from WorkerMeta that is not in config but in DB
            emptyQueueSleepTime = 2000 // time in milliseconds a worker should sleep if all of his queues are empty
            pauseSleepTime = 5000 // time in milliseconds a worker should sleep if he is paused
            manualPoolName = "manualPool"
            // generic worker pool
            genericPool {
                workers = 3
                queueNames = ["UberjobsDefaultQueue"]
            }
            // only working on jobs that use a browser; 1 worker ensures that we don't try to create 2 browsers at the same time.
            usingBrowserPool {
                workers = -1
                queueNames = ['testJobQueue', 'scheduledTestQueue']
            }
        }
        scheduling {
            thread {
                active = true
            }
        }
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    root {
        info('stdout')
    }

    warn 'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate',
            'spring.BeanBuilder' // for output from initialization

    debug 'grails.plugin.uberjobs'
}

cors.url.pattern = '/*'