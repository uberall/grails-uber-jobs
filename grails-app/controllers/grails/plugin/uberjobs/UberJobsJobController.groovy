package grails.plugin.uberjobs

class UberJobsJobController extends AbstractUberJobsController {

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
        } else if (json.args == null) {
            renderBadRequest([errors: [args: 'NULLABLE']])
            return
        }

        // we double check queue as we dont want empty string to be used as queuenames
        String queue = null
        if(json.queue)
            queue = json.queue as String

        def result = uberJobsJobService.enqueue(json.job.toString(), json.args as List, queue)

        renderResponse([succes: result.validate()])
    }

    def delete() {
        //TODO: implement me!
    }

    def update() {
        //TODO: implement me!
    }
}
