package grails.plugin.uberjobs

import grails.converters.JSON
import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class UberTriggerMeta implements UberApiResponseObject {

    /**
     * The job that this trigger belongs to
     */
    static belongsTo = [job: UberJobMeta]

    /**
     * do not store arguments (as list, we will store them as a json blob)
     */
    static transients = ['arguments']

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
     * when this trigger was last fired
     */
    DateTime estimatedNextExecution

    /**
     * date of creation and last update
     */
    DateTime dateCreated, lastUpdated

    /**
     * The arguments that will be passed into perform
     */
    String argumentsJSON

    transient List arguments = []

    List getArguments() {
        if (!arguments && argumentsJSON)
            arguments = JSON.parse(argumentsJSON) as List
        arguments
    }

    void beforeValidate() {
        argumentsJSON = ((arguments) as JSON).toString()
        updateEstimatedNextExecution()
    }

    void updateEstimatedNextExecution(){
        estimatedNextExecution = new CronExpression(cronExpression).getNextValidTimeAfter(DateTime.now())
    }

    static constraints = {
        name unique: true, nullable: false
        cronExpression nullable: false, validator: { val ->
            if (!CronExpression.isValidExpression(val))
                'INVALID_EXPRESSION'
        }
        queueName nullable: false
        job nullable: false
        lastFired nullabe: true
        estimatedNextExecution nullable: true
        argumentsJSON nullable: false
    }

    static mapping = {
        dateCreated type: PersistentDateTime
        lastUpdated type: PersistentDateTime
        lastFired type: PersistentDateTime
        estimatedNextExecution type: PersistentDateTime
        argumentsJSON column: 'arguments', type: 'text'
    }

    @Override
    Map toResponseObject() {
        [
                id            : id,
                name          : name,
                queueName     : queueName,
                cronExpression: cronExpression,
                nextFire      : new CronExpression(cronExpression).getNextValidTimeAfter(DateTime.now()).millis,
                enabled       : enabled,
                lastFired     : lastFired?.millis,
                dateCreated   : dateCreated?.millis,
                lastUpdated   : lastUpdated?.millis,
        ]
    }
}
