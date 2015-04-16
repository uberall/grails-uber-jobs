package grails.plugin.uberjobs

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

/**
 * A List of items to work on, which can be enabled and disabled on the fly
 */
class UberQueue {

    /**
     * The list of items to perform
     */
    static hasMany = [items: UberJob]

    /**
     * The name of the Queue
     */
    String name

    /**
     * Whether or not this Queue should be worked on currently
     */
    boolean enabled

    /**
     * Creation and update dates
     */
    DateTime dateCreated, lastUpdated

    static constraints = {
        name nullable: false, unique: true
        items nullable: true
    }

    static mapping = {
        dateCreated type: PersistentDateTime
        lastUpdated type: PersistentDateTime
    }
}
