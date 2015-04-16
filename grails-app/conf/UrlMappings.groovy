class UrlMappings {

    static mappings = {

        "/uberjobs/jobs"(controller: 'uberJob', action: [GET: 'list', POST: 'enqueue'], parseRequest: true)
        "/uberjobs/jobs/$id"(controller: 'uberJob', action: [GET: 'get', DELETE: 'delete', PUT: 'update'], parseRequest: true)
        "/uberjobs/jobmetas"(controller: 'uberJobMeta', action: [GET: 'list'], parseRequest: true)
        "/uberjobs/jobmetas/$id"(controller: 'uberJobMeta', action: [GET: 'get', PUT: 'update'], parseRequest: true)
        "/uberjobs/triggers"(controller: 'uberTriggerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/triggers/$id"(controller: 'uberTriggerMeta', action: [GET: 'get', PUT: 'update', DELETE: 'delete'], parseRequest: true)
        "/uberjobs/workers"(controller: 'uberWorkerMeta', action: [GET: 'list', POST: 'create'], parseRequest: true)
        "/uberjobs/workers/$id"(controller: 'uberWorkerMeta', action: [GET: 'get'], parseRequest: true)
        "/uberjobs/workers/$id/pause"(controller: 'uberWorkerMeta', action: [PUT: 'pause'], parseRequest: true)
        "/uberjobs/workers/$id/stop"(controller: 'uberWorkerMeta', action: [PUT: 'stop'], parseRequest: true)

    }
}
