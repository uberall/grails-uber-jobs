package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class UberTriggerMeta {

    static belongsTo = [job: UberJobMeta]

    String name
    String cronExpression
    String queueName
    boolean enabled = false

    DateTime dateCreated, lastUpdated

    static constraints = {
        name unique: true, nullable: false
        cronExpression nullable: false
        queueName nullable: false
        job nullable: false
    }

    static mapping = {
        dateCreated type: PersistentDateTime
        lastUpdated type: PersistentDateTime
    }

}
