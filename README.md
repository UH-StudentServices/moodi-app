Moodi
================

Moodi is an integration application that automates course creation and user enrollment to Moodle based on master data from Sisu.

Integrations
================

- Sisu (Master data source for courses and course students and teachers)
- Moodle (Target for modification operations by Moodi. Course spaces are created and users are 
enrolled via Moodle api by Moodi application)

Environments
================

Currently Moodi has 3 environments: local, dev, and prod. Integrations are mapped for each environment in corresponding application-<env>.yml. 

Requirements
---------------

The following programs must be installed:
- JDK 8

Local development
---------------

This is an integration application and as such running locally requires a lot of setup work with ssh tunnels, certificates etc. The 
recommended way to test changes to the application locally is by running automated tests. Therefore good automated test coverage is essential
when new features are added.

### Running tests
```sh
./gradlew test
# or
./gradlew test_psql # To run against a local Dockerized Postgres, see below.
```

### Running Moodi-Moodle integration tests
This is normally done in Jenkins, but if you want to test something locally, you need to go through 
an ssh tunnel via moodi-dev, because the Moodle API has an IP restriction in place.

Open an ssh tunnel to call the Moodle API:
```
ssh moodi-dev -L 1444:moodi-2-moodle-20.student.helsinki.fi:443
```
Into /etc/hosts:
```
127.0.0.1       moodi-2-moodle-20.student.helsinki.fi 
```

Then run the tests
```
# Get the token from moodi-dev:/opt/moodi/config/moodi.properties
read MOODLE_WS_TOKEN 
./gradlew itest --rerun-tasks -Pmoodle_base_url=https://moodi-2-moodle-20.student.helsinki.fi:1444/moodlecurrent/webservice/rest/server.php -Pmoodle_ws_token=$MOODLE_WS_TOKEN   
```

### Running Moodi locally
#### Prerequisites
Open an ssh tunnel to call Sisu through the test API GW and the Moodle API:
```
ssh  moodi-1.student.helsinki.fi -L 1443:gw-api-test.it.helsinki.fi:443 -L 1444:moodi-2-moodle-20.student.helsinki.fi:443
```

The file ~/moodi/moodi.properties needs to exist:
```
httpClient.keystoreLocation: /path/to/moodi-app/moodi.p12
httpClient.keystorePassword: <see moodi-dev:/opt/moodi/config/moodi.properties>
integration.moodle.wstoken: <see moodi-dev:/opt/moodi/config/moodi.properties>
```
Copy moodi-dev:/opt/moodi/config/keystore/moodi.p12 this directory.

Into /etc/hosts: 
```
127.0.0.1       gw-api-test.it.helsinki.fi   
127.0.0.1       moodi-2-moodle-20.student.helsinki.fi 
```

application-dev.yml:
```
  moodle:
    baseUrl: https://moodi-2-moodle-20.student.helsinki.fi:1444/moodlecurrent

  sisu:
    baseUrl: https://gw-api-test.it.helsinki.fi:1443/secure/sisu
    apiKey: <see https://api-test.it.helsinki.fi/portal/applications/f2d7ede8-2810-4a1e-97ed-e828100a1e4a/subscriptions?subscription=e959b407-023e-499a-99b4-07023e399adb#s>
```

#### Starting Moodi
```sh
./gradlew bootRun
```
The test UI is now available at http://localhost:8084/login
Credentials are: user/password

#### Cleaning up
- Comment out the line in /etc/hosts.
- Delete local moodi.p12
- Remove the keystore password and Moodle token from ~/moodi/moodi.properties

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
