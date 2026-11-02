# Unified Document Viewer

A Spring Boot application that consolidates vehicle documents from two independent dealership systems: Sales and Service.

The application accepts a VIN, retrieves documents from both systems in parallel, and returns a unified document list. It also supports database snapshot fallback when an external API is unavailable.

## Architecture

The project consists of three Gradle modules:

| Module | Description |
|---|---|
| `document-viewer` | Main REST API that aggregates documents from both external APIs. |
| `sales-mock` | Mock Sales System API. |
| `service-mock` | Mock Service System API. |

### Request Flow

1. The client sends a VIN to the Document Viewer API.
2. The application validates the VIN.
3. Sales and Service APIs are called concurrently.
4. If an API is unavailable, the application attempts to retrieve its latest snapshot from PostgreSQL.
5. Documents from both sources are merged into a unified response.
6. The API returns the consolidated documents and the status of each source.

### Resilience

- **Parallel requests:** Sales and Service APIs are called concurrently using `CompletableFuture`.
- **Timeout handling:** External API connection and read timeouts are configured.
- **Independent fallback:** Each source can independently fall back to its latest database snapshot.
- **Partial availability:** If one source is unavailable, documents from the healthy source are still returned.
- **Service unavailable:** Returns HTTP 503 when neither source has usable data.

## Technology Stack

- Java 21
- Spring Boot
- Spring MVC
- Spring RestClient
- CompletableFuture
- Spring Data JPA
- PostgreSQL
- Gradle
- Docker Compose
- Springdoc OpenAPI / Swagger UI
- JUnit 6, Mockito, AssertJ

## Prerequisites

Make sure you have installed:

- JDK 21
- Docker and Docker Compose
- Git

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd <project-directory>
```

### 2. Start PostgreSQL

Start the database using Docker Compose:

```bash
docker compose up -d
```

Default database configuration:

| Property | Value |
|---|---|
| Database | `documentviewer` |
| Username | `keyloop` |
| Password | `keyloop` |
| Port | `5432` |

The application uses the following environment variables, with defaults provided:

```properties
DB_URL=jdbc:postgresql://localhost:5432/documentviewer
DB_USERNAME=keyloop
DB_PASSWORD=keyloop
```

### 3. Build the project

Build all three modules:

```bash
./gradlew clean build
```

### 4. Run the applications

Start each module in a separate terminal.

**Terminal 1 – Sales Mock API**

```bash
./gradlew :sales-mock:bootRun
```

**Terminal 2 – Service Mock API**

```bash
./gradlew :service-mock:bootRun
```

**Terminal 3 – Document Viewer API**

```bash
./gradlew :document-viewer:bootRun
```

### Application Ports

| Application | Port |
|---|---:|
| Document Viewer | 8081 |
| Service Mock | 8082 |
| Sales Mock | 8083 |

## API Endpoints

### Document Viewer API

**Search vehicle documents**

```http
GET /api/v1/documents?vin={vin}
```

Example:

```bash
curl "http://localhost:8081/api/v1/documents?vin=1FTFW1ET5DFC10312"
```

Successful response:

```json
{
  "vin": "1FTFW1ET5DFC10312",
  "documents": [],
  "sources": [
    {
      "source": "SALES",
      "status": "OK",
      "message": null
    },
    {
      "source": "SERVICE",
      "status": "OK",
      "message": null
    }
  ]
}
```

Possible source statuses:

| Status | Description |
|---|---|
| `OK` | Data retrieved successfully from the external API. |
| `TIMEOUT` | External API request timed out and no snapshot was available. |
| `UNAVAILABLE` | External API is unavailable and no snapshot was available. |
| `STALE` | Data retrieved from a previously stored database snapshot. |

### Mock APIs

| Method | Endpoint | Module |
|---|---|---|
| GET | `/api/sales/vehicles/{vin}/documents` | `sales-mock` |
| GET | `/api/service/vehicles/{vin}/documents` | `service-mock` |

## OpenAPI Documentation

The Document Viewer API provides interactive API documentation through Swagger UI.

Once the application is running, access:

- **Swagger UI:** http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8081/v3/api-docs

## Testing

Run unit tests for the Document Viewer module:

```bash
./gradlew :document-viewer:test
```

To force Gradle to execute all tests again:

```bash
./gradlew :document-viewer:test --rerun-tasks
```

The test report is generated at:

```text
document-viewer/build/reports/tests/test/index.html
```

## Project Structure

```text
.
├── sales-mock/
├── service-mock/
├── document-viewer/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   └── resources/
│       └── test/
├── compose.yaml
├── build.gradle
├── settings.gradle
└── gradlew
```


