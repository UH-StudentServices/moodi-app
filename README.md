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

Currently Moodi has 4 environments: local, dev, (qa*) and prod. Integrations are mapped for each environment in corresponding application-<env>.yml. 
Currently only IAM has a mock implementatation that is used in local and dev environments.

*) Will be removed.

Requirements
---------------

The following programs must be installed:
- JDK 8

Running locally
---------------
### 1. Prerequisites

This is an integration application and as such running locally requires a lot of setup work with ssh tunnels, certificates etc. The 
recommended way to test changes to the application locally is by running automated tests. Therefore good automated test coverage is essential
when new features are added.

### 2. Running tests
```sh
./gradlew test
# or
./gradlew test_psql # To run against a local Dockerized Postgres, see below.
```

### 3. Starting the server

The file ~/moodi/moodi.properties needs to exist.

```sh
./gradlew bootRun
```

# Database

## Servers
The server environments use Postgres 9.4.

## Local
The local environment and tests use a H2 database. 

### Dockerized local Postgres

Use this, if you want to test DB changes locally with Postgres. For example, if you want to test column type changes with existing 
data in the DB.

The DB is populated with a dump from Moodi dev environment.

Start the DB by running 
```
docker-compose up
```

To clear the data, first stop the container and then:
```
docker rm moodi-postgres
docker volume rm moodi-app_moodi-postgres-data 
```
To create a new dump:

```
ssh moodi-1.student.helsinki.fi
sudo su - postgres
pg_dump -x -f moodi_dump.sql moodi
# Then copy the new dump file over docker-entrypoint-initdb.d/moodi_dump.sql
```
