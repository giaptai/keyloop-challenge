# Scenario D Unified Document Viewer

> A Unified Document Viewer to provide a single view of all 
> documents related to a vehicle by connecting to two different (mocked) dealership systems.

## Contents

- [Overview](#Overview)
- [Prerequisites](#Prerequisites)
- [Build](#Build)
- [Run](#Run)
- [Test](#Test)

## Overview
| Module            | Port | Role                                      | Calls / Uses                                           |
| ----------------- | ---- | ----------------------------------------- | ------------------------------------------------------ |
| `document-viewer` | 8081 | Main REST API, aggregates documents       | `sales-mock`, `service-mock` (in parallel), PostgreSQL |
| `sales-mock`      | 8082 | Mock Sales System API                     | —                                                      |
| `service-mock`   | 8083 | Mock Service System API                   | —                                                      |
| PostgreSQL        | 5432 | Stores latest snapshot per VIN and source | Used by `document-viewer`                              |

## Prerequisites
- **JDK 21** – Required to build and run the application.
- **Docker (28.x.x) and Docker compose** - run and start the PostgreSQL container.
- **Gradle 9.x.x** - build project and run inividual application modules

## Build

Check `settings.gradle` to know name of module

```bash
./gradlew clean build # build 3 modules
./gradlew :document-viewer:build # build 1 module (optional)
./gradlew clean build -x test # build without testing

# Output jars are in `*/build/libs/`.
```

**Note**
> this commands above is for window env the command maybe diffrent on other Os

## Run
1. Start PostgreSQL and keep it running:

   ```bash
   docker compose up -d
   ```

   `docker compose stop` stops the container and keeps data. `docker compose down` removes the container; the `pgdata` volume keeps the data.
   
   Check `docker-compose.yml` to see the configuration

2. Start the three applications, each in its own terminal:

   ```bash
   ./gradlew :sales-mock:bootRun
   ./gradlew :service-mock:bootRun
   ./gradlew :document-viewer:bootRun
   ```

3. Build everything (optional):

   ```bash
   ./gradlew clean build
   ```

Database settings (development defaults, in `document-viewer` configuration): database `documentviewer`, user `keyloop`, password `keyloop`, port `5432`.

## Test

### Mock data
Number of documents per VIN in the mock data: [service](service-mock/src/main/resources/mock-data/service-documents.json), [sales](sales-mock/src/main/resources/mock-data/sales-documents.json)

| VIN | SALES | SERVICE |
|---|---:|---:|
| 1HGCM82633A004352 | 3 | 2 |
| 1FTFW1ET5DFC10312 | 2 | 3 |
| WBA3A5C50CF256985 | 2 | 0 |
| 5YJSA1E26HF000337 | 2 | 0 |
| 2T1BURHE5JC123456 | 1 | 0 |
| JH4KA9650MC000001 | 0 | 1 |
| JTDKB20U340012345 | 0 | 0 |

1. Run test with Junit

    Run the tests for the `document-viewer` module: check file `DocumentViewerBusinessTest.java`

    ```bash
    ./gradlew :document-viewer:test
    ```
    HTML report link:

    `document-viewer/build/reports/tests/test/index.html`

2. Run test with OpenAPI

   - Swagger UI (while document-viewer is running): http://localhost:8081/swagger-ui/index.html
   - OpenAPI JSON: http://localhost:8081/v3/api-docs

3. Manual tests with cURL

    Normal request (both sources OK):
    ```bash
    curl "http://localhost:8081/api/v1/documents?vin=1FTFW1ET5DFC10312"
    ```

    Invalid VIN (expect 400):
    ```bash
    curl "http://localhost:8081/api/v1/documents?vin=ABC"
    ```

    No data for this VIN in either source (expect 200 with empty documents):
    ```bash
    curl "http://localhost:8081/api/v1/documents?vin=JTDKB20U340012345"
    ```

    Mock APIs directly:
    ```bash
    curl "http://localhost:8082/api/sales/vehicles/1FTFW1ET5DFC10312/documents"
    curl "http://localhost:8083/api/service/vehicles/1FTFW1ET5DFC10312/documents"
    ```