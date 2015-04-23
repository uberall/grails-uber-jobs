package grails.plugin.uberjobs

import org.springframework.http.HttpStatus

class AbstractUberJobsController {

    def uberJobsJobService

    def beforeInterceptor = {
        if(!config.frontend.enabled){
            renderNotFound()
            return false
        }

        normalizeParameters()
        true
    }

    def grailsApplication

    protected normalizeParameters(){
        if(!params.max){
            params.max =  params.getInt('max') ?: config.frontend.params.max ?: 20
            params.offset = params.getInt('offset') ?: 0
        }
    }

    protected renderResponse(def response) {
        def contentType = 'application/json'
        if (config.frontend.responseType == 'XML') {
            contentType = 'application/xml'
        }
        render(contentType: contentType) {
            response
        }
    }

    protected renderBadRequest(def object){
        response.status = HttpStatus.BAD_REQUEST.value()
        renderResponse(object)
    }

    protected renderErrorResponse(def object){
        def errors = [:]
        object.errors.allErrors.each {
            errors << [(it.field): it.codes.last().toUpperCase() ?: 'INVALID']
        }
        renderBadRequest([error: errors])
    }

    protected renderNotFound(){
        response.status = HttpStatus.NOT_FOUND.value()
        renderResponse([error: 'Requested resource not found'])
    }

    protected getConfig() {
        grailsApplication.config.grails.uberjobs
    }

    protected withDomainObject(def domainClass, Closure c){
        def object = domainClass.get(params.getLong("id"))
        if(!object){
            renderNotFound()
        } else {
            c.call object
        }
    }
}
