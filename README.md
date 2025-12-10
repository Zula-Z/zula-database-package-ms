# zula-database-library

This repository provides a Spring Boot library for database access using JDBI (not JPA/Hibernate).

Overview
- The library exposes a `Jdbi` bean and a `DataSourceTransactionManager` for use in Spring applications.
- Uses JDBI 3 (SqlObject plugin supported) for DAO implementation and mapping.

Key changes / migration notes
- Reliance on JPA/Hibernate has been removed. Do NOT include `spring-boot-starter-data-jpa` in projects that depend on this library.
- The application using this library must provide a `DataSource` (via Spring Boot `spring.datasource.*` properties or custom configuration).

Required dependencies (pom.xml)
- Remove:
  - `org.springframework.boot:spring-boot-starter-data-jpa`
  - Any `hibernate-*` artifacts
- Add:
  - `org.springframework.boot:spring-boot-starter-jdbc`
  - `org.jdbi:jdbi3-core`
  - `org.jdbi:jdbi3-sqlobject`
  - (Optional) `org.jdbi:jdbi3-postgres` or other dialect helpers if needed
  - `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` (if Flyway migrations are used; version 11.0.0+ to support PostgreSQL 17)

Example pom dependency snippet

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
  </dependency>

  <dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-core</artifactId>
    <version>3.37.0</version>
  </dependency>

  <dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-sqlobject</artifactId>
    <version>3.37.0</version>
  </dependency>

  <!-- Optional DB-specific support -->
  <dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-postgres</artifactId>
    <version>3.37.0</version>
  </dependency>
</dependencies>
```

Configuration
- `DatabaseAutoConfig` (in this library) exposes two beans:
  - `Jdbi jdbi(DataSource)` — configured with `SqlObjectPlugin`.
  - `DataSourceTransactionManager transactionManager(DataSource)` — enables Spring `@Transactional` with the provided DataSource.

Example application properties (Spring Boot)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: secret
  flyway:
    enabled: true
```

Example SqlObject DAO

```java
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;

public interface MessageDao {
  @SqlUpdate("INSERT INTO messages(id, text) VALUES (:id, :text)")
  void insert(@BindBean Message message);

  @SqlQuery("SELECT id, text FROM messages WHERE id = :id")
  @RegisterBeanMapper(Message.class)
  Message findById(@Bind("id") String id);
}
```

Using DAOs from Spring

```java
@Service
public class MessagingService {
  private final MessageDao messageDao;

  public MessagingService(Jdbi jdbi) {
    this.messageDao = jdbi.onDemand(MessageDao.class);
  }

  public void send(Message m) {
    messageDao.insert(m);
  }
}
```

Removing JPA auto-configuration (if required)

If any upstream dependency still pulls in Spring Data JPA, disable JPA auto-configuration in the application's properties:

```
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

Consumer setup (pom.xml and application.yml)

Add the following dependencies to a consuming project's `pom.xml` (adjust versions as needed and/or manage Spring Boot via the parent `spring-boot-starter-parent`):

```xml
<!-- Spring Boot JDBC starter (version normally managed by the parent) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
  <version>2.7.0</version>
</dependency>

<!-- JDBI core and SqlObject support (matches this library) -->
<dependency>
  <groupId>org.jdbi</groupId>
  <artifactId>jdbi3-core</artifactId>
  <version>3.37.1</version>
</dependency>
<dependency>
  <groupId>org.jdbi</groupId>
  <artifactId>jdbi3-sqlobject</artifactId>
  <version>3.37.1</version>
</dependency>
<dependency>
  <groupId>org.jdbi</groupId>
  <artifactId>jdbi3-spring5</artifactId>
  <version>3.37.1</version>
</dependency>

<!-- Add the JDBC driver for the target database (example: PostgreSQL) -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.5.4</version>
</dependency>

<!-- Optional: Flyway if database migrations are used by the consumer -->
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
  <version>11.0.0</version>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
  <version>11.0.0</version>
</dependency>
```

Example `application.yml` for a consuming Spring Boot service:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: secret
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

# Optional: disable JPA auto-configuration if some other dependency brings it in
# spring:
#   autoconfigure:
#     exclude: org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

Validation / smoke tests
- Build with `mvn -DskipTests=false clean package` and confirm no JPA/Hibernate classes remain on the classpath.
- Add a simple integration test that uses an in-memory datasource, obtains the `Jdbi` bean, creates a test DAO, and performs CRUD operations.

Recommended next steps
- Ensure the application that consumes this library provides a `DataSource` and includes the required dependencies.
- Confirm database migrations create the `message_inbox` and `message_outbox` tables (or adjust table names in DAOs).
- Add integration tests (H2 or testcontainers) to validate mappings and DAOs.

Support
- For mapping issues with Java 8+ date/time types, register appropriate JDBI plugins or mappers in the application's configuration.
- For assistance adding build/test automation or further pom adjustments, update the project accordingly.
