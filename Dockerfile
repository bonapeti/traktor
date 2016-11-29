FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/traktor.jar /traktor/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/traktor/app.jar"]
