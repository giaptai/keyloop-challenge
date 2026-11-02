# Unified Document Viewer

A Spring Boot backend that returns all documents for a vehicle (by VIN) by combining two dealership systems, Sales and Service, called in parallel. If one system is down, it falls back to the last saved copy from PostgreSQL.

## Modules

| Module | Role | Port |
|---|---|---|
| `document-viewer` | Main REST API: aggregates, persists snapshots | 8081 |
| `sales-mock` | Mock Sales System API (JSON data) | 8082 |
| `service-mock` | Mock Service System API (JSON data) | 8083 |

## Requirements

- JDK 21
- Docker and Docker Compose
- Gradle wrapper included (`./gradlew`, or `gradlew.bat` on Windows)

## Running locally

1. Start PostgreSQL and keep it running:

   ```bash
   docker compose up -d
   ```

   `docker compose stop` stops the container and keeps data. `docker compose down` removes the container; the `pgdata` volume keeps the data.

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

## API

### Search documents by VIN

```http
GET /api/v1/documents?vin={vin}
```

```bash
curl "http://localhost:8081/api/v1/documents?vin=1FTFW1ET5DFC10312"
```

Response shape (shortened; the real response for this VIN contains 5 documents: 2 SALES and 3 SERVICE):
```json
{
  "vin": "1FTFW1ET5DFC10312",
  "documents": [
    {
      "documentId": "SAL-006",
      "vin": "1FTFW1ET5DFC10312",
      "title": "Vehicle Purchase Agreement",
      "documentType": "PURCHASE_AGREEMENT",
      "status": "SIGNED",
      "referenceNumber": "SO-2026-00310",
      "createdAt": "2026-03-12T14:00:00",
      "source": "SALES"
    }
  ],
  "sources": [
    { "source": "SALES", "status": "OK", "message": null },
    { "source": "SERVICE", "status": "OK", "message": null }
  ]
}
```

`documents` is merged from all usable sources and sorted by `createdAt`. Each document has a `source` field.

Each source in `sources` reports one status:

| Status | Meaning |
|---|---|
| `OK` | Fetched successfully now; snapshot updated. |
| `STALE` | Fetch failed, but a saved snapshot exists; its documents are returned and `message` gives the fetch time. |
| `TIMEOUT` | Fetch exceeded the timeout and no snapshot exists. |
| `UNAVAILABLE` | Fetch failed for another reason and no snapshot exists. |

### Error responses

All errors use the same body:

```json
{
  "timestamp": "2026-11-02T00:15:10.120000",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid VIN format. VIN must be 17 characters and exclude I, O, Q."
}
```

| HTTP | When |
|---|---|
| 200 | At least one source is `OK` or `STALE`. |
| 400 | VIN is missing or not 17 valid characters. |
| 503 | Neither source is usable (fetch failed and no snapshot). |
| 500 | Unexpected error (generic handler). |

### Mock APIs

| Method | Path | Module |
|---|---|---|
| GET | `/api/sales/vehicles/{vin}/documents` | `sales-mock` |
| GET | `/api/service/vehicles/{vin}/documents` | `service-mock` |

### Swagger UI

With `document-viewer` running: http://localhost:8081/swagger-ui/index.html (OpenAPI JSON at `/v3/api-docs`).

## Design

- **Parallel calls:** Sales and Service are called concurrently on the `aggregationExecutor` thread pool (`ExecutorConfig`), then joined.
- **Timeouts:** set on the HTTP client (`RestClient` with `SimpleClientHttpRequestFactory`) using `aggregation.timeout-ms`. A timed-out call releases its thread.
- **Partial results:** one failing source does not fail the request.
- **Snapshots:** after each successful fetch, the result is saved per `(vin, source)` in PostgreSQL (`document_snapshot`). A failed fetch falls back to that snapshot, reported as `STALE`.
- **Database as auxiliary:** if PostgreSQL is down, reads and writes of snapshots are skipped and the response still uses the healthy source.
- **Connection pool:** Hikari, maximum 10 connections, 1 s wait for a free connection.

## Testing

```bash
./gradlew :document-viewer:test
```

Unit tests (`DocumentViewerBusinessTest`, Mockito + JUnit 5, 10 tests) cover: both sources OK with merge and sort; one source failing with and without snapshot; both failing (503); invalid VIN (400); read timeout; connect timeout; connection refused; database failure while a healthy source still responds.

Report: `document-viewer/build/reports/tests/test/index.html`

## Project structure

```text
.
├── document-viewer/   main service
├── sales-mock/        mock Sales API, data in src/main/resources/mock-data/
├── service-mock/      mock Service API, data in src/main/resources/mock-data/
├── docker-compose.yml
├── build.gradle
├── settings.gradle
└── gradlew
```

## Known limitations

- Snapshots are saved after a successful fetch, possibly asynchronously, so they can briefly lag the latest data.
- Concurrent writes for the same VIN and source may finish out of order.
- Snapshots have no maximum age; an old snapshot is still returned as `STALE`.
- Schema is managed by `ddl-auto` rather than a migration tool.
- Mock data is static JSON; mocks do not simulate delays or failures on demand.
- Credentials are in configuration for local development only.
- Pool sizes (20 threads, 10 DB connections) are estimates and should be measured under load.

## AI collaboration

I used a generative AI assistant while building this. I set the scope and architecture, asked for one step at a time (mock data, mock services, aggregation, persistence, tests), and compiled and ran each piece before accepting it. The assistant made several mistakes that I found and corrected, including using an enum value for a status, a misspelled status constant, a dropped field during mapping, and a `null` result from a failed future. I kept the design small on purpose: one snapshot table, no separate history table, and no extra result classes.