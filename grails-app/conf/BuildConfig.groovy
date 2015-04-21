grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.fork = [
        test   : false,
        run    : false,
        war    : false,
        console: false
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "https://raw.github.com/peh/errbuddy-plugins/mvn-repo"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        runtime('mysql:mysql-connector-java:5.1.27') {
            export = false
        }
        compile "org.jadira.usertype:usertype.jodatime:2.0.1"

    }

    plugins {
        build(":release:3.0.1", ":rest-client-builder:1.0.3") {
            export = false
        }

        build(':tomcat:7.0.55') {
            export = false
        }

        runtime (":cors:1.1.6"){
            export = false
        }

        // hibernate is needed just for joda-time persistence, we either need to remove joda-time or find a better way to work around this
        compile(':hibernate4:4.3.6.1')
        compile ":joda-time:1.5"

    }
}
