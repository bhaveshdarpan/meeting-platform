# meeting-webhook-service

Brief service that implements the meeting platform webhook handling and related application logic.

**Build & Run**

- **Prerequisites**: Java 17+ (JDK), Git. The project uses the Gradle wrapper so you do not need a local Gradle install.
- **Build (Windows)**: `gradlew.bat build`
- **Build (Unix/macOS)**: `./gradlew build`
- **Run (development with Gradle)**: `gradlew.bat bootRun` or `./gradlew bootRun`
- **Run the built JAR**: `java -jar build/libs/*.jar`
- **Run tests**: `gradlew.bat test` or `./gradlew test`

Configuration

- Application configuration is in `src/main/resources/application.properties` and can be overridden with environment variables or Spring `--spring.*` properties. The app defaults to port 8080 unless overridden via `server.port`.

**Quick Dev Tips**

- Use the Gradle wrapper (`gradlew` / `gradlew.bat`) to ensure consistent builds across machines.
- To run on Windows PowerShell:

```powershell
# from repo root
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

**Architecture Overview**

The project follows a layered architecture with clear package separation:

- `com.github.meeting_platform.application`: Application services and orchestration (use-cases).
- `com.github.meeting_platform.domain`: Core domain models, business rules, and domain services.
- `com.github.meeting_platform.infrastructure`: Adapters and integrations (persistence, external HTTP clients, etc.).
- `com.github.meeting_platform.config`: Spring configuration and beans.
- `com.github.meeting_platform.common`: Shared utilities and exception types.
- `com.github.meeting_platform.application.events` and `application.eventhandler`: Event definitions and simple in-process event handlers used for decoupling side effects.

This layout keeps domain logic independent of framework and infrastructure concerns, improving testability and clarity.

**Design Decisions & Trade-offs**

- Layered, DDD-influenced structure: I favored explicit separation of concerns (application, domain, infrastructure) to make business rules easy to test and change. Trade-off: more files and indirection for simple features.
- Spring Boot + Gradle wrapper: provides fast developer feedback loops and a familiar ecosystem. Trade-off: increased dependency surface and startup overhead compared to minimal frameworks.
- In-process event handling: events are handled inside the JVM (see `application.events` / `eventhandler`) to keep the initial implementation simple and easy to test. Trade-off: this approach is easier to run locally but less resilient than using an external message broker for high scale or cross-process delivery.
- Default to simple configuration and no bundled database migrations: keeps the repository lightweight and easy to run locally. Trade-off: additional work is required to harden production deployments (DB migrations, externalized config, secrets, observability).

**Assumptions**

- Developers have JDK 17+ installed and can run the Gradle wrapper.
- Local development uses the embedded or in-memory test configurations; production deployments will provide external infrastructure (database, message queues) and configure them via properties or environment variables.
- The service listens on the standard Spring Boot port (8080) unless overridden.

**Next Steps / Suggestions**

- Add infra integration tests and a sample `docker-compose` for local integration (DB, message broker).
- Introduce a durable message broker (Kafka/RabbitMQ) if cross-process event delivery and at-least-once guarantees are required.
- Add DB migration tooling (Flyway/Liquibase) and production-specific configuration profiles.

If you'd like, I can also add a `CONTRIBUTING.md`, a sample `docker-compose.yml`, or scaffold a basic `Makefile`/PowerShell script to simplify common developer tasks.

Added Assets

- `Dockerfile`: multi-stage Docker build that compiles the project with Gradle and produces a small runtime image.
- `docker-compose.yml`: runs the app container on port `8080` (app-only composition as requested).
- `.github/workflows/ci.yml`: GitHub Actions workflow that checks out the repo, sets up JDK 17, and runs `./gradlew build` on pushes and pull requests to `main`.

Using docker-compose

Build and run the app with Docker Compose:

```bash
docker compose build
docker compose up
```

Or run in detached mode:

```bash
docker compose up -d --build
```

The service will be available at http://localhost:8080 unless `server.port` is overridden.

CI Notes

The included GitHub Actions workflow runs a full Gradle build and tests on `push` and `pull_request` events targeting `main`. You can extend it with caching, artifact upload, or deployment steps as needed.
