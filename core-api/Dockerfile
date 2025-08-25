FROM openjdk:21-jdk-slim AS core-api-base

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get --no-install-recommends install -yq \
      vim

FROM core-api-base AS core-api

ARG ACTIVE_PROFILE

WORKDIR /app

COPY build.gradle settings.gradle gradlew /app/

COPY . /app

RUN chmod +x gradlew
RUN ./gradlew clean build -Pprofile=${ACTIVE_PROFILE}

CMD ["java", "-jar", "build/libs/core-api-0.0.1.jar"]