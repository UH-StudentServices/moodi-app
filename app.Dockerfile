# This file is meant to be used for dockerized local development and testing with
# https://github.com/UH-StudentServices/moodi-development project.
FROM gradle:7.6-jdk11

USER gradle

WORKDIR /app

# Copy the Gradle configuration files to download dependencies
COPY build.gradle gradlew gradle.properties /app/
COPY gradle /app/gradle
COPY gradle-build /app/gradle-build

# Download dependencies as a separate step to leverage Docker caching
RUN gradle build -g /home/gradle --exclude-task test --continue --no-daemon

# Copy the rest of the code
COPY . /app

EXPOSE 8080

CMD gradle bootRun -g /home/gradle --no-daemon --args="--spring.profiles.active=local_docker"
