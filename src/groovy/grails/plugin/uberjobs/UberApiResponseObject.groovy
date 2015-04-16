package grails.plugin.uberjobs

/**
 * Interface for objects that are returned over the api
 */
interface UberApiResponseObject {

    /**
     * transforms the implementing object into a map to return over the api
     * @return
     */
    Map toResponseObject()
}