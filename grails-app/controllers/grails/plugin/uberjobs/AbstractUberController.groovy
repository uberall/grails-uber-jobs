package grails.plugin.uberjobs

import grails.validation.Validateable
import org.springframework.http.HttpStatus

class AbstractUberController {

    def beforeInterceptor = {
        if(!config.frontend.enabled)
            return false
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

    protected renderErrorResponse(def object){
        response.status = HttpStatus.BAD_REQUEST.value()
        renderResponse([error: object.errors.allErrors.field])
    }

    protected renderNotFound(){
        response.status = HttpStatus.NOT_FOUND.value()
        renderResponse([error: 'Requested resource not found'])
    }


    protected getConfig() {
        grailsApplication.config.uberjobs
    }
}
