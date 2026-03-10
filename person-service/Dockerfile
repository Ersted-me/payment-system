FROM gradle:jdk25 as build

WORKDIR /build

COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY openapi ./openapi

COPY person-service-app/build.gradle.kts ./person-service-app/
COPY person-service-sdk/build.gradle.kts ./person-service-sdk/

RUN gradle dependencies --no-daemon

COPY person-service-app/src ./person-service-app/src

RUN gradle :person-service-app:openApiGenerate :person-service-app:bootJar --no-daemon


FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

USER app:app

COPY --from=build /build/person-service-app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar app.jar"]