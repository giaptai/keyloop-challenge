## System Design Document – Unified Document Viewer

### 1. Architecture diagram
![Overview](docs/keyloop-exercise-overview.drawio.png)

### 2. Module description
| Component | Responsibility |
|---|---:|
| Client | send request to retrieve documents by VIN |
| Document Viewer | validate request, call 2 external apis, aggregate documents, handle errors and take a snapshot |
| Sales Mock API | provide data from Sales system |
| Service Mock API | provide data from Service system |
| PostgreSQL (docker-compose) | store snapshot by VIN and source |

### 3. Data flow
![document-viewer-flow](docs/keyloop-exercise-document_viewer_sequence-flow.drawio.png)

![apimock-flow](docs/docs/keyloop-exercise-apimock-sequence-flow.drawio.png)

### 4. Technologies stack
| Technology | Why choose |
|---|---|
| Java 21 | Provides a modern, stable Java LTS environment. |
| Spring Boot 4 | Has embedded server, auto-configuration and dependencies |
| Spring MVC | Provides the REST controller layer and JSON serialization for the API. |
| Spring RestClient | Synchronous HTTP client, for call external Sales and Service APIS |
| Executor | pool of reusale threads, so call don't create and destroy a thread each time |
| CompletableFuture  | call Sales and Service API seperate and combine of their result |
| PostgreSQL 16  | stores document snapshot persistently |
| Docker Compose | Simplifies local PostgreSQL setup and management. | 
| HikariCP | Default in Spring Boot, create a pool connect to DB can reusable |
| Springdoc OpenAPI | Generates the OpenAPI specification and Swagger UI from the controller code. |
| JUnit 6 | Supports automated testing of business logic |
| Gradle (multi-module) | manages multi-modules, help them can run separately easier  |

### 5. Observability strategy
- Logs (SLF4J) show each request’s result and any failure while the application runs. Every source reports a string status (OK, STALE, TIMEOUT, UNAVAILABLE) in the response, so the caller can see which source failed.

### 6. AI assist in the design phase 
I used AI at serveral stages:
- Analyse the assignment requirements
- Generate `DocumentViewerBusinessTest` Unit Test class
- While checking concurrent desgin i noiced that a pool of only to threads do not guarantee parallel calls when servel requests arriive at the same time, so I have an idea is increas pre-thread in pool and ask AI my options is good or bad, what is the trade off
- Sometime it proposed more than the requirements needed such as history table - when i used Postgresql for fallback is external api is down.