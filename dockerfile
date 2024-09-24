FROM eclipse-temurin:17-jdk-alpine
COPY target/debricked-scan-0.0.1-SNAPSHOT.jar debricked-scan.jar
ENTRYPOINT ["java", "-jar", "debricked-scan.jar"]