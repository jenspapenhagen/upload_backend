# build step
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn clean package

# release step
FROM bellsoft/liberica-runtime-container:jdk-21-slim-musl AS release
WORKDIR /app
VOLUME /app/uploadfiles

# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

COPY --from=build /build/target/lib/* /app/lib/
COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8080
USER 1001
ENTRYPOINT ["java", "-jar", "app.jar"]
