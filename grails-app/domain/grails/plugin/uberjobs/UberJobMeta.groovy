package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class UberJobMeta {

    static hasMany = [triggers: UberTriggerMeta]

    // the job class that is associated to this JobMeta information
    String job

    // whether WorkEntries with this JobMeta should be worked on
    boolean enabled = false

    // whether more than one worker at a time is allowed to work on this job type
    boolean singletonJob = false

    // the minimum time between two executions of this job
    int minDelay = 0

    // The time this JobMeta was last updated
    DateTime lastUpdated

    // the earliest time WorkEntries with this JobMeta is allowed to be worked on
    DateTime earliestNextExecution

    static constraints = {
        job unique: true, nullable: false
        earliestNextExecution nullable: true
        triggers nullable: true
    }

    static mapping  = {
        lastUpdated type: PersistentDateTime
        earliestNextExecution type: PersistentDateTime
    }

}
