package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class UberTriggerMeta {

    /**
     * The job that this trigger belongs to
     */
    static belongsTo = [job: UberJobMeta]

    /**
     * The unique name of this trigger
     */
    String name

    /**
     * The CronExpression
     */
    String cronExpression

    /**
     * The queueName of the Queue this trigger will enqueue jobs in
     * it is not a relational link as the queue can be deleted and will be recreated
     */
    String queueName

    /**
     * Whether or not this trigger should be fired if cron expression is expired
     */
    boolean enabled = false

    /**
     * when this trigger was last fired
     */
    DateTime lastFired

    /**
     * date of creation and last update
     */
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
        lastFired type: PersistentDateTime
    }

}
