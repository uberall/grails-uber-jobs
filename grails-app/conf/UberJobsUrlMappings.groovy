class UberJobsUrlMappings {

    static mappings = {

        "/uberjobs/api/jobs"(controller: 'uberJob', action: [GET: 'list', POST: 'enqueue'], parseRequest: true)
        "/uberjobs/api/jobs/$id"(controller: 'uberJob', action: [GET: 'get', DELETE: 'delete', PUT: 'update'], parseRequest: true)
        "/uberjobs/api/jobmetas"(controller: 'uberJobMeta', action: [GET: 'list'], parseRequest: true)
        "/uberjobs/api/jobmetas/$id"(controller: 'uberJobMeta', action: [GET: 'get', PUT: 'update'], parseRequest: true)
        "/uberjobs/api/triggers"(controller: 'uberTriggerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/api/triggers/$id"(controller: 'uberTriggerMeta', action: [GET: 'get', PUT: 'update', DELETE: 'delete'], parseRequest: true)
        "/uberjobs/api/workers"(controller: 'uberWorkerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/api/workers/$id"(controller: 'uberWorkerMeta', action: [GET: 'get'], parseRequest: true)
        "/uberjobs/api/workers/$id/pause"(controller: 'uberWorkerMeta', action: [PUT: 'pause'], parseRequest: true)
        "/uberjobs/api/workers/$id/stop"(controller: 'uberWorkerMeta', action: [PUT: 'stop'], parseRequest: true)
        "/uberjobs/api/queues"(controller: 'uberQueue', action: [GET: 'list'])
        "/uberjobs/api/queues/$id"(controller: 'uberQueue', action: [GET: 'get', PUT: 'update', DELETE: 'delete'], parseRequest: true)
        "/uberjobs/api/queues/$id/clear"(controller: 'uberQueue', action: [GET: 'clear'])
    }
}
