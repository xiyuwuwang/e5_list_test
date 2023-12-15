FROM openjdk:8-jdk-alpine
VOLUME /tm
ADD dd-yunjia-api.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
