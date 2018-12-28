FROM openjdk:8

ADD target/traktor-engine-0.0.1-SNAPSHOT.jar /app/traktor-engine.jar
CMD ["java", "-jar","/app/traktor-engine.jar"]