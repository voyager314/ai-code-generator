# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot service. Application code lives under `src/main/java/com/yzy`, organized by layer: `controller`, `service`, `service/impl`, `mapper`, `entity`, `dto`, `vo`, `config`, `aop`, `annotation`, `exception`, and AI-specific packages under `ai`. Resource files live in `src/main/resources`: MyBatis XML mappers are in `mapper/`, prompt templates are in `prompt/`, and `application-example.yml` documents required local configuration. Tests belong in `src/test/java`, mirroring the main package structure.

## Build, Test, and Development Commands

Use the Maven wrapper from the repository root:

- `.\mvnw.cmd test` runs the JUnit/Spring test suite.
- `.\mvnw.cmd package` compiles, tests, and builds the application artifact under `target/`.
- `.\mvnw.cmd spring-boot:run` starts the service locally.

Create `src/main/resources/application.yml` from `application-example.yml` before running locally. Keep real database, Redis, AI, OSS, and Pexels credentials out of Git.

## Coding Style & Naming Conventions

Use 4-space indentation and standard Java naming: `PascalCase` for classes, `camelCase` for methods and fields, and uppercase constants. Keep controllers thin, put business logic in `service/impl`, and keep persistence contracts in `mapper` interfaces plus matching XML under `resources/mapper`. Follow existing suffixes such as `*Controller`, `*Service`, `*ServiceImpl`, `*Mapper`, `*Request`, and `*VO`. Lombok is already used; prefer existing annotations and patterns before introducing new boilerplate.

## Testing Guidelines

The project uses `spring-boot-starter-test` with JUnit 5. Name test classes after the class or feature under test, ending in `Tests` or `Test`, and place them under the matching `src/test/java/com/yzy/...` package. Add focused unit tests for services and integration tests for controller or persistence changes. Run `.\mvnw.cmd test` before submitting changes.

## Commit & Pull Request Guidelines

No readable local commit history is available in this checkout, so use concise imperative commit subjects such as `Add app paging validation` or `Fix Redis session config`. Keep commits focused by feature or fix.

Pull requests should include a short summary, test results, related issue links when available, and screenshots or API examples for user-facing endpoint changes. Call out configuration changes, database schema updates in `src/main/java/com/yzy/sql`, and any new required secrets.

## Security & Configuration Tips

Do not commit `application.yml`, generated `target/` files, IDE metadata, logs, or credentials. Update `application-example.yml` whenever a new required property is added so local and deployment setup stays reproducible.
