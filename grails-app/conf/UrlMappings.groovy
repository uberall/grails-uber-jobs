class UrlMappings {

	static mappings = {

        "/uberjobs/jobs"(controller: 'uberJob', action: [GET: 'list', POST: 'enqueue'])
        "/uberjobs/jobs/$id"(controller: 'uberJob', action: [GET: 'get', DELETE: 'delete', PUT: 'update'])
        "/uberjobs/jobMetas"(controller: 'uberJobMeta', action: [GET: 'list'])
        "/uberjobs/jobMetas/$id"(controller: 'uberJobMeta', action: [GET: 'get', PUT: 'update'])

	}
}
