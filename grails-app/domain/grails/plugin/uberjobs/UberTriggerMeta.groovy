package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class UberTriggerMeta implements UberApiResponseObject {

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
        lastFired nullabe: true
    }

    static mapping = {
        dateCreated type: PersistentDateTime
        lastUpdated type: PersistentDateTime
        lastFired type: PersistentDateTime
    }

    @Override
    Map toResponseObject() {
        [
                id            : id,
                name          : name,
                queueName     : queueName,
                cronExpression: cronExpression,
                enabled       : enabled,
                lastFired     : lastFired?.millis,
                dateCreated   : dateCreated?.millis,
                lastUpdated   : lastUpdated?.millis,
                // TODO: add nextFire here (should be the parsed cron expression no?)

        ]
    }
}
