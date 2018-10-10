PR TEST


Moodi
================

Moodi is an integration application that automates course creation and user enrollment to Moodle based on master data from Oodi and IAM.

Integrations
================

- Oodi (Master data source for courses and course students and teachers)
- Moodle (Target for modification operations by Moodi. Course spaces are created and users are enrolled via Moodle api by Moodi application)
- IAM (Used for mapping student and employee numbers that Moodi gets from Oodi to usernames. Usernames can then be mapped to Moodle user accounts)

Environments
================

Currently Moodi has 4 environments: local, dev, qa and prod. Integrations are mapped for each environment in corresponding application-<env>.yml. 
Currently only IAM has a mock implementatation that is used in local and dev environments.

Requirements
---------------

The following programs must be installed:
- JDK 8

Add the following empty properties to ~/.gradle/gradle.properties:

```
opintoni_artifactory_base_url=
opintoni_artifactory_username=
opintoni_artifactory_password=
```

These properties need to be defined but are only used by CI builds run by Jenkins.

Running locally
---------------
### 1. Prerequisites

This is an integration application and as such running locally requires a lot of setup work with ssh tunnels, certificates etc. The 
recommended way to test changes to the application locally is by running automated tests. Therefore good automated test coverage is essential
when new features are added.

### 2. Running tests
```sh
./gradlew test
```

### 3. Starting the server
```sh
./gradlew bootRun
```
