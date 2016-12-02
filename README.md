Moodi
================

Requirements
---------------

The following programs must be installed:
- JDK 8

Running locally
---------------
### 1. Prerequisites
Add the following empty properties to ~/.gradle/gradle.properties:

```
opintoni_artifactory_base_url=
opintoni_artifactory_username=
opintoni_artifactory_password=
```

These properties need to be defined but are only used by CI builds run by Jenkins.

Additionally, the presence of `${user.home}/moodi/moodi.properties` config file is required at startup time. This external config file should contain all sensitive properties necessary for the consumption of external APIs. In effect, the file should specify a value for `integration.moodle.wstoken` property.

### 2. Running tests
```sh
./gradlew test
```

### 3. Starting the server
```sh
./gradlew bootRun
```
