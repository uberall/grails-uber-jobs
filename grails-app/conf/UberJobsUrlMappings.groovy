class UberJobsUrlMappings {

    static mappings = {

        "/uberjobs/api/jobs"(controller: 'uberJobsJob', action: [GET: 'list', POST: 'enqueue'], parseRequest: true)
        "/uberjobs/api/jobs/$id"(controller: 'uberJobsJob', action: [GET: 'get', DELETE: 'delete', PUT: 'update'], parseRequest: true)
        "/uberjobs/api/jobs/$id/failure"(controller: 'uberJobsJob', action: [GET: 'failure'], parseRequest: true)
        "/uberjobs/api/jobmetas"(controller: 'uberJobsJobMeta', action: [GET: 'list'], parseRequest: true)
        "/uberjobs/api/jobmetas/$id"(controller: 'uberJobsJobMeta', action: [GET: 'get', PUT: 'update'], parseRequest: true)
        "/uberjobs/api/triggers"(controller: 'uberJobsTriggerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/api/triggers/$id"(controller: 'uberJobsTriggerMeta', action: [GET: 'get', PUT: 'update', DELETE: 'delete'], parseRequest: true)
        "/uberjobs/api/workers"(controller: 'uberJobsWorkerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/api/workers/$id"(controller: 'uberJobsWorkerMeta', action: [GET: 'get'], parseRequest: true)
        "/uberjobs/api/workers/$id/pause"(controller: 'uberJobsWorkerMeta', action: [PUT: 'pause'], parseRequest: true)
        "/uberjobs/api/workers/$id/resume"(controller: 'uberJobsWorkerMeta', action: [PUT: 'resume'], parseRequest: true)
        "/uberjobs/api/workers/$id/stop"(controller: 'uberJobsWorkerMeta', action: [PUT: 'stop'], parseRequest: true)
        "/uberjobs/api/queues"(controller: 'uberJobsQueue', action: [GET: 'list'])
        "/uberjobs/api/queues/$id"(controller: 'uberJobsQueue', action: [GET: 'get', PUT: 'update', DELETE: 'delete'], parseRequest: true)
        "/uberjobs/api/queues/$id/clear"(controller: 'uberJobsQueue', action: [GET: 'clear'])
    }
}
