FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
COPY mvnw pom.xml ./
# The repository may be checked out on Windows with CRLF line endings.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/main ./src/main
RUN ./mvnw -B -ntp -Dmaven.test.skip=true package \
    && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --home-dir /app --no-create-home --shell /usr/sbin/nologin app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar

USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
# Documentation only: Spring reads Render's PORT at runtime (8080 locally).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
