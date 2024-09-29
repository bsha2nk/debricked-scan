### Creating instances
1. Use the docker-compose.yml to spin up instances of the 2 Java applications, a MySQL database and a RabbitMQ message broker. The images will be pulled from docker hub, so there is no need to build the image first. Command "docker compose up".
2. Required environement variables:
   a. jwt-token: a valid jwt token for the application to connect to debricked with.
   b. spring.mail.username, spring.mail.password: a gmail smtp enabled username and app password
   c. slack-bot-token, slack-channel-id: a bot auth token and the channel id it can send notification messages to
   d. email-recipient: email address to which notification mails will be sent

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
6. The status system will poll debricked's status endpoint at a fixed interval (30s default) and update the status in the database.
7. If all scans are complete then the system will not poll the status API and will print "All scans completed." in the console periodically.
8. Notifications will be sent to the Slack channel and email id given in docker-compose in the following scenarios
   a. File upload success/failure.
   b. Scan started/failed to start.
   c. Scan in progress.
   d. Scan completed with number of vulnerabilities. The rule engine only sends a notification for number of vulnerabilities if it exceeds 3 (hardcoded).

### Architecture
Of the 2 Java services, one is used to receive files from the use, upload it to Debricked, start the scan and save scan details to the MySQL database. In order to send notifications at various stages of scans it uses RabbitMQ message broker to communicate with the other Java service which is the notification system. The notification service receives the event and message and sends an email and a Slack message. If either email or Slack message fails then it does not acknwoledge the RabbitMQ message so that it can be re-tried again. If both notifications were sent successfully then the message in the message queue is removed.

![Architecture](https://github.com/user-attachments/assets/a63f0926-25f7-4300-8ca1-f066cae16f34)
