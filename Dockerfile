# build step
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder
COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn clean package

# release step
FROM bellsoft/liberica-runtime-container:jdk-21-slim-musl AS release

WORKDIR /app
VOLUME /app/uploadfiles

COPY --from=builder /build/target/quarkus-app/ /app

# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
