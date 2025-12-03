# build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY adapter/build.gradle.kts adapter/
COPY application/build.gradle.kts application/
COPY domain/build.gradle.kts domain/
RUN chmod +x gradlew
RUN ./gradlew --no-daemon build || return 0

COPY adapter/src adapter/src
COPY application/src application/src
COPY domain/src domain/src
RUN ./gradlew --no-daemon adapter:bootJar \
    && mv ./adapter/build/libs/*.jar ./adapter/build/libs/app.jar



# run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/adapter/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar /app/app.jar"]