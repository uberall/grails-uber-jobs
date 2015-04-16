package grails.plugin.uberjobs

class UberJobController extends AbstractUberController {

    static allowedMethods = [list: 'GET', get: 'GET', enqueue: 'POST', delete: 'DELETE', update: 'PUT']

    def list() {
        UberJob.Status status = UberJob.Status.OPEN // by default only show OPEN jobs
        if (params.status) {
            try {
                status = UberJob.Status.valueOf(params.status.toString())
            } catch (Exception e) {
                log.error("$params.status can not be converted to UberJob.Status", e)
            }
        }
        def jobs = UberJob.createCriteria().list(params) {
            eq('status', status)
        }
        renderResponse([list: jobs, total: jobs.totalCount])
    }

    def enqueue() {
        //TODO: implement me!
    }

    def delete() {
        //TODO: implement me!
    }

    def update() {
        //TODO: implement me!
    }
}
