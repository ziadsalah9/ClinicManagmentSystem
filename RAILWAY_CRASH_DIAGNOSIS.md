# Railway deployment crash diagnosis

Date: 2026-11-12
Railway project: `empathetic-celebration`
Service: `ClinicManagmentSystem`
Environment: `production`
Crashed deployment: `0bf45801-d7bc-471d-ae5e-1c8a6ce25178`
Commit: `32fa048` (`update port in .prop`)

Resolved deployment: `e3a9a331-a012-4ba9-b009-81e0da74e103`
Resolution: applied the 1 GB memory optimization and deployed it successfully.

## Verdict

The backend was not being stopped by a Spring exception, a PostgreSQL error, or a Redis error.

It was most likely being **OOM-killed because the service was running with a 1 GB memory limit while the Java process needed more than that during startup**. Railway did not expose an explicit kernel OOM line or exit code in the captured logs, so this is the leading diagnosis rather than a directly logged confirmation.

The optimization removed the Spring Modulith runtime/observability startup scan, unused Redis, production DEBUG logging, SQL logging, and oversized pools, then capped the JVM for a 1 GB container. The new deployment started in `7.181s`, is `Online`, and is using about `318 MB` of its `1,024 MB` limit.

Railway reports:

| Metric | Value |
|---|---:|
| Memory limit | 1,024 MB |
| Recent 1-day average | ~920 MB |
| Recent 1-day current | ~1,123 MB |
| Recent 1-day peak | ~1,395 MB |
| 7-day current | ~1,547 MB |
| 7-day peak | ~3,259 MB |
| New deployment current | ~318 MB |
| New deployment peak | ~319 MB |

The recent 1-day peak is 1.395 GB and the deploy-time sample was 1.096 GB. The 3.26 GB peak covers a longer window and may include older deployments or build activity, but the 1 GB limit was still exceeded during the old startup. The new deployment stayed near 318 MB after startup.

The container is killed while Spring is still starting, then Railway restarts it. The deployment logs show the application restarting roughly every 8–12 seconds and always stopping in the middle of startup.

## Evidence from the logs

The last lines of every startup attempt stop at the same stage, before the application finishes starting:

- No `Started ClinicBackendApplication`
- No `Tomcat started on port`
- No Hikari/PostgreSQL connection message
- No Flyway migration message
- No `APPLICATION FAILED TO START`
- No `Caused by:` exception
- No Redis connection exception

The last visible activity is Spring bean creation / Spring Modulith / ArchUnit startup. The JVM then disappears without printing an exception. That is the normal signature of an operating-system OOM kill.

The log also contains many `DEBUG` messages because this line is enabled in `src/main/resources/application.properties`:

```properties
logging.level.org.springframework=DEBUG
```

That was added for deployment debugging. It made startup slower and produced a large amount of log data, but it was not the root cause. The root cause was memory usage against the 1 GB limit. The new deployment uses `INFO` logging and `spring.jpa.show-sql=false`.

## What is not the problem

- **Build**: Railway build completed successfully (`BUILD SUCCESS`), and the image was pushed.
- **PostgreSQL variables**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` are present.
- **Redis variables**: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, and `SPRING_DATA_REDIS_PASSWORD` are present. Spring Data Redis initializes without a connection error before the kill.
- **JWT secret**: `SECURITY_JWT_SECRET_BASE64` is present.
- **Port conflict**: The app reads `PORT`, and Railway exposes the service on port 8080. The process is not failing with `Address already in use`.
- **Database backup**: The repository's `backup.sql` contains `flyway_schema_history` with successful V1–V9 rows and the `event_publication` table, so the restore appears consistent.

## Latest detailed analysis and applied 1 GB plan

Analysis date: 2026-11-12. The approved 1 GB optimization has been applied and deployed.

### What in the application is using memory

The crash is most likely memory/container-limit exhaustion during startup, not a database or Redis error and not a large data load in application code.

- The app never reaches Hikari, Flyway, Tomcat, or `Started ClinicBackendApplication`. The logs stop during bean creation with no application exception, so database and Redis are not the cause of this crash.
- The main startup-specific consumer is `spring-modulith-runtime` (`pom.xml:110`). Its `ApplicationModulesBootstrap` starts an asynchronous `ApplicationModulesFactory` on `cTaskExecutor-1`, and ArchUnit scans the application classpath on every startup. This is visible in the Railway logs and happens even though Modulith verification is not explicitly enabled.
- Modulith observability also creates `moduleTracingBeanPostProcessor`, which depends on `ApplicationModulesRuntime` and keeps runtime module metadata active.
- `logging.level.org.springframework=DEBUG` (`application.properties:72`) and `spring.jpa.show-sql=true` add startup overhead and a large amount of log output.
- The Docker entrypoint uses `-XX:MaxRAMPercentage=75.0` on a 1 GB service. That allows roughly a 750 MB heap and leaves too little for metaspace, thread stacks, direct memory, and native libraries.
- There is no obvious large startup query, cache, or `findAll` in the application code. `AdminBootstrapRunner` is an `ApplicationRunner`, so it runs only after startup completes. `LocalOtpStore` is `local`-profile only and is not populated at startup.
- CPU is not the bottleneck: recent 1-day CPU averaged `0.0028 vCPU` with a `0.16 vCPU` maximum.
- Local `hs_err_pid*.log` files show native `mmap` failures with only about 72 MB of heap used, which supports native/OS memory exhaustion rather than a Java heap data leak.

### Remediation options

#### Option A - raise memory first (not used)

1. Raise the Railway service memory to at least 2 GB; use 4 GB for the first stable boot if available.
2. Redeploy and confirm the application reaches `Started ClinicBackendApplication`, Hikari, Flyway, and Tomcat.
3. Then measure memory again before deciding whether 1 GB is viable.
4. Remove production DEBUG logging and add conservative JVM flags:
   - `-XX:MaxRAMPercentage=60`
   - `-XX:MaxMetaspaceSize=256m`
   - `-XX:ActiveProcessorCount=2`
   - `-XX:+ExitOnOutOfMemoryError`

This is the lowest-risk path because it does not remove application features.

#### Option B - optimize to fit within 1 GB (applied)

Applied because the service must stay at 1 GB:

1. Removed `spring-modulith-runtime`, Modulith observability, Modulith actuator, and the Modulith JPA starter.
2. Replaced `spring-modulith-starter-core` with `spring-modulith-api` so module annotations still compile; kept the Modulith test starter.
3. Removed the unused Redis starter and Redis test starter.
4. Disabled Springdoc/Swagger by default in production.
5. Set Spring logging to `INFO` and `spring.jpa.show-sql=false`.
6. Reduced Hikari, Tomcat, email executor, and scheduling pool sizes for five users.
7. Capped the JVM with `-Xms128m -Xmx448m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m -Xss512k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError`.

Verification: `mvn -B -Dtest='!ClinicBackendApplicationTests' test` passed 99 tests with 2 Docker-dependent tests skipped; `mvn -B -DskipTests package` succeeded. The full suite's only error was `ClinicBackendApplicationTests` because Docker Desktop was not running.

#### Option C - reproduce before changing anything (not needed)

The deployed result already confirmed the optimization is viable within the 1 GB limit.

### Decision made

The 1 GB optimization path was chosen and implemented. Railway memory was not increased.

## Applied fix and deployment result

Applied on 2026-11-12 without increasing the Railway memory limit.

Deployment `e3a9a331-a012-4ba9-b009-81e0da74e103`:

- Status: `SUCCESS`
- Service status: `Online`
- Startup: `Started ClinicBackendApplication in 7.181 seconds`
- Tomcat: `started on port 8080`
- Hikari: connected to PostgreSQL
- Flyway: validated 9 migrations; schema already at version 9
- Memory: about `318 MB` current and `319 MB` peak against a `1,024 MB` limit (31.1%)
- CPU: `0.0 vCPU` current at the final check; `0.4917 vCPU` maximum during startup

Railway environment variables `SPRINGDOC_API_DOCS_ENABLED=false` and `SPRINGDOC_SWAGGER_UI_ENABLED=false` were set with deploys skipped, then the service was deployed with `railway up`.

### Known non-memory issue: mail health check

`/actuator/health` returns `503 DOWN`, while `/actuator/health/liveness` and `/actuator/health/readiness` return `200 UP`. The logs show the mail health indicator failing:

```text
Mail health check failed
Couldn't connect to host, port: smtp.gmail.com, 587; timeout 5000
java.net.SocketTimeoutException: Connect timed out
```

This is a pre-existing outbound SMTP timeout, not a memory or startup failure. The application is online, but doctor invitation email delivery from Railway may fail until SMTP egress is fixed or mail health is disabled for monitoring.

## Database backup check

The old crashed deployment stopped before database migration. The new deployment reached Hikari and Flyway successfully.

The repository's `backup.sql` was inspected and contains:

- `flyway_schema_history` with successful rows for V1–V9
- `event_publication`, used by Spring Modulith
- the expected application tables and data

That means the restore looks consistent. The new deployment reported:

```text
Successfully validated 9 migrations
Current version of schema "public": 9
Schema "public" is up to date. No migration necessary.
```

To double-check the history, run:

```sql
SELECT count(*) FROM flyway_schema_history WHERE success = false;
```

This should return `0`. If Hibernate validation fails in the future, investigate Flyway/Hibernate errors. Do **not** solve schema problems with `spring.jpa.hibernate.ddl-auto=update`.

If Hibernate reports `Schema validation: missing table [event_publication]`, the uploaded database did not actually come from this backup.

## Useful commands

Check the latest deployment status:

```powershell
railway status --json
```

Read the current deployment logs:

```powershell
railway logs --json --lines 300
```

Check current memory usage:

```powershell
railway metrics --memory --json
```

Check deployment history:

```powershell
railway deployment list --json
```

## Bottom line

The 1 GB optimization succeeded. Deployment `e3a9a331-a012-4ba9-b009-81e0da74e103` is online, started in `7.181s`, and is using about `318 MB` of the `1,024 MB` limit. The original crash was most likely memory-limit exhaustion during the Modulith/ArchUnit startup scan, not a database or Redis error. The remaining known issue is the Gmail SMTP health-check timeout at `smtp.gmail.com:587`, which affects `/actuator/health` and possibly invitation email delivery.
