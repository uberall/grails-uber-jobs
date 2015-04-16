class UrlMappings {

    static mappings = {

        "/uberjobs/jobs"(controller: 'uberJob', action: [GET: 'list', POST: 'enqueue'], parseRequest: true)
        "/uberjobs/jobs/$id"(controller: 'uberJob', action: [GET: 'get', DELETE: 'delete', PUT: 'update'], parseRequest: true)
        "/uberjobs/jobmetas"(controller: 'uberJobMeta', action: [GET: 'list'], parseRequest: true)
        "/uberjobs/jobmetas/$id"(controller: 'uberJobMeta', action: [GET: 'get', PUT: 'update'], parseRequest: true)

    }
}
