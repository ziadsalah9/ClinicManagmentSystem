# Undo prompt: revert the 1 GB Railway memory optimization

Use this file when you want to restore the application to its previous dependency and runtime configuration.

## Prompt for the next assistant

Revert the 1 GB Railway memory optimization in the `clinic-backend` project exactly as described below. Preserve all unrelated local changes, including `RAILWAY_CRASH_DIAGNOSIS.md`. Do not change Railway memory settings unless the user explicitly asks. After restoring the files, run `mvn -B test` and `mvn -B -DskipTests package`, then report the results. Do not commit or push unless the user explicitly asks.

## Files to restore

### 1. `pom.xml`

Restore `spring-boot-starter-data-redis` immediately after `spring-boot-starter-data-jpa`:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
```

Replace the single `spring-modulith-api` dependency with the original Modulith dependencies:

```xml
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-observability-api</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-starter-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-starter-jpa</artifactId>
		</dependency>
```

Restore the runtime Modulith dependencies immediately after the PostgreSQL dependency:

```xml
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-actuator</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-observability-core</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.modulith</groupId>
			<artifactId>spring-modulith-runtime</artifactId>
			<scope>runtime</scope>
		</dependency>
```

Restore the Redis test starter immediately after `spring-boot-starter-data-jpa-test`:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis-test</artifactId>
			<scope>test</scope>
		</dependency>
```

Keep `spring-modulith-bom` and `spring-modulith-starter-test` unchanged.

### 2. `src/main/resources/application.properties`

Restore the server, datasource, JPA, and Flyway block to:

```properties
server.port=${PORT:8083}
server.address=0.0.0.0

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/clinic_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Remove the added Hikari and Tomcat tuning lines:

```properties
server.tomcat.threads.max=10
server.tomcat.threads.min-spare=2
server.tomcat.max-connections=50
server.tomcat.accept-count=20
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=60000
spring.datasource.hikari.max-lifetime=1800000
```

Remove the added task executor properties:

```properties
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=4
spring.task.execution.pool.queue-capacity=50
spring.task.scheduling.pool.size=1
```

Restore the Swagger/OpenAPI block by removing the two environment-controlled enabling properties:

```properties
springdoc.api-docs.enabled=${SPRINGDOC_API_DOCS_ENABLED:false}
springdoc.swagger-ui.enabled=${SPRINGDOC_SWAGGER_UI_ENABLED:false}
```

Keep the remaining `springdoc.swagger-ui.*` settings unchanged.

Restore the logging block to:

```properties
# Force Spring Boot to show full error stack trace on crash
logging.level.org.springframework=DEBUG
logging.exception-conversion-word=%wex
```

If Railway variables `SPRINGDOC_API_DOCS_ENABLED` or `SPRINGDOC_SWAGGER_UI_ENABLED` were added, remove them or set both to `true`.

### 3. `src/main/java/com/clinic/backend/config/AsyncConfiguration.java`

Restore the email executor sizes to:

```java
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
```

### 4. `Dockerfile`

Restore the original JVM entrypoint:

```dockerfile
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

The optimized entrypoint being reverted is:

```dockerfile
ENTRYPOINT ["java", "-Xms128m", "-Xmx448m", "-XX:MaxMetaspaceSize=192m", "-XX:ReservedCodeCacheSize=96m", "-XX:MaxDirectMemorySize=64m", "-Xss512k", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
```

## Verification after restoring

Run:

```powershell
mvn -B test
mvn -B -DskipTests package
```

Expected result: the project compiles and the test suite passes. If a test requires Redis or Modulith runtime behavior, confirm the restored dependencies are present before investigating test failures.

## Note

With the original configuration restored, the application will again use the Modulith runtime/ArchUnit startup scan and the original JVM memory settings. It may exceed a 1 GB Railway limit again. Do not deploy the restored version to a 1 GB service without the user's explicit approval.
