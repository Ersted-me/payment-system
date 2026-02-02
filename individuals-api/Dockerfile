FROM gradle:8.14.3-jdk24 as build

WORKDIR /build

COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY openapi ./openapi

RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon


FROM eclipse-temurin:24-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

USER app:app

COPY --from=build /build/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar app.jar"]