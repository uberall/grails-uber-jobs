// configuration for plugin testing - will not be included in the plugin zip

grails.project.groupId = 'grails.plugin.uberjobs'

grails {
    uberjobs {
        enabled = true // enable the plugin itself
        jobs {
            update = true // enable updating JobMeta on application startup
            cleanup = true // enable pruning JobMeta of Jobs that are not found in code anymore
        }
        triggers {
            update = true // enable updating TriggerMeta on application startup
            cleanup = true // enable pruning TriggerMeta of Triggers that are not found in code anymore
        }
        workers {
            update = true // enable updating of WorkerMeta on application startup
            cleanup = false // enable pruning of WorkerMeta that is not in config anymore
            restart = true // enable starting from WorkerMeta that is not in config but in DB
            // generic worker pool
            genericPool {
                workers = 3
                queueNames = "testJobQueue"
            }
            // only working on jobs that use a browser; 1 worker ensures that we don't try to create 2 browsers at the same time.
            usingBrowserPool {
                workers = -1
                queueNames = ['testJobQueue', 'scheduledTestQueue']
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
