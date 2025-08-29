# --- Build Stage ---
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first to leverage caching
COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY build.gradle settings.gradle /app/

# Copy all source code
COPY src ./src

# Build the application
RUN chmod +x gradlew
RUN ./gradlew clean build -x test -Pprofile=${ACTIVE_PROFILE}

# --- Runtime Stage ---
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port (if applicable, based on application.yml)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]