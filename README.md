Moodi
================

Requirements
---------------

The following programs must be installed
- JDK 8

Running locally
---------------
### 1. Prerequisites
Add the following empty properties to ~/.gradle/gradle.properties
Properties need to be defined but are only required at automated build server Jenkins

```
opintoni_artifactory_base_url=
opintoni_artifactory_username=
opintoni_artifactory_password=
```

### 2. Execute tests
```sh
./gradlew test
```

### 4. Run backend locally
```sh
./gradlew bootRun
```
