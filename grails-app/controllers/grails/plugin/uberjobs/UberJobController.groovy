package grails.plugin.uberjobs

class UberJobController extends AbstractUberController {

    static allowedMethods = [list: 'GET', get: 'GET', enqueue: 'POST', delete: 'DELETE', update: 'PUT']

    def list() {
        List statuses
        if (params.status) {
                statuses = [UberJob.Status.valueOf(params.status.toString())]

        } else if (params.getList("status[]")) {
            statuses = params.getList("status[]").collect { UberJob.Status.valueOf(it.toString()) }
        }
        else {
            statuses = [UberJob.Status.OPEN] // only show OPEN by default
        }
        def jobs = UberJob.createCriteria().list(params) {
            inList('status', statuses)
        }
        renderResponse([list: jobs, total: jobs.totalCount])
    }

    def enqueue() {
        def json = request.JSON
        if (!json.job) {
            renderBadRequest([errors: [job: 'NULLABLE']])
            return
        } else if (!json.args) {
            renderBadRequest([errors: [args: 'NULLABLE']])
            return
        }
        def result = uberJobsService.enqueue(json.job.toString(), json.args as List)
        renderResponse([succes: result.validate()])
    }

    def delete() {
        //TODO: implement me!
    }

    def update() {
        //TODO: implement me!
    }
}
