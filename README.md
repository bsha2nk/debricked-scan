### Creating instances
1. Use the dockerfile to create a docker image of the Java springboot application.Command "docker build --tag=debricked-scan --force-rm=true .".
2. Use the docker-compose.yml to spin up instances of the Java application and a MySQL database. Command "docker compose up".
3. You must pass in a valid jwt token as an environment variable in the docker compose file so that a task can be scheduled to check the status of scans. If this is not provided then although you can upload and start scans, the status will not be updated and you will see HTTP 401 errors in the Java app's console.

### Making requests
1. You can upload files to the app at "http://localhost:8080/api/v1/files/upload".
2. Using Content-Type of multipart/form-data in an HTTP client like Postman, you need to provide 4 mandatory parameters.
   - "files" : the files you want to upload
   - "jwtToken" : a valid jwt token for the application to connect to debricked with.
   - "repositoryName" : repository to associate with this upload
   - "commitName" : commit to associate with this upload
3. If a request to upload is successful it will automatically send a request to start a scan. You should see a message akin to "HTTP 200 Files were uploaded successfully and scan started with repositoryId 123 and commitId 456 and ci-uploadId 789."
4. Once a file is uploaded and its scan started, a record with its details and audit timestamps will be saved in the MySQL database with status set to "start".
5. If for some reason a file upload wasn't successful then a scan will not be started and you will be shown an error message.
6. There is no notification system to update the user about the status of scans yet, but you should see some log statements in the Java app's console about the status.
7. The status system will poll debricked's status endpoint every 10s and update the status in the database.
8. If all scans are complete then the system will not poll the status API and will print "All scans completed." in the console periodically.
