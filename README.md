# zula-database-library

This repository provides a Spring Boot library for database access using JDBI (not JPA/Hibernate).

Overview
- The library exposes a `Jdbi` bean and a `DataSourceTransactionManager` for use in Spring applications.
- Uses JDBI 3 (SqlObject plugin supported) for DAO implementation and mapping.

- This repository is the MySQL-only variant (`zula-database-library-ms`) that ships with the MySQL JDBC driver and Flyway MySQL support. `DatabaseManager#createQueueSchemaAndTables` only emits MySQL DDL (`AUTO_INCREMENT`, `DATETIME`, `ENGINE=InnoDB`) and therefore requires the consuming service to connect to MySQL. Keep `zula.database.auto-create-queue-schema` enabled so the library manages queue schema creation for each service without manual migrations.

Key changes / migration notes
- Reliance on JPA/Hibernate has been removed. Do NOT include `spring-boot-starter-data-jpa` in projects that depend on this library.
- The application using this library must provide a `DataSource` (via Spring Boot `spring.datasource.*` properties or custom configuration).

For MySQL services keep `zula.database.auto-create-queue-schema` enabled (the default) so `DatabaseManager#createQueueSchemaAndTables` creates each service-specific queue schema and its `message_inbox`/`message_outbox` tables with the MySQL statements shown above. Consumer code does not need to worry about the underlying SQL unless you disable the auto-create flag and provide your own migrations.

Required dependencies (pom.xml)
- Remove:
  - `org.springframework.boot:spring-boot-starter-data-jpa`
  - Any `hibernate-*` artifacts
- Add:
  - `org.springframework.boot:spring-boot-starter-jdbc`
  - `org.jdbi:jdbi3-core`
  - `org.jdbi:jdbi3-sqlobject`
  - `org.jdbi:jdbi3-spring5`
  - `com.mysql:mysql-connector-j`
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-mysql`

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

  <dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-spring5</artifactId>
    <version>3.37.0</version>
  </dependency>

  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
  </dependency>

  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>11.0.0</version>
  </dependency>

  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>11.0.0</version>
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

Add the following dependencies to a consuming project's `pom.xml` (adjust versions as needed or manage Spring Boot via the parent `spring-boot-starter-parent`):

```xml
<!-- Spring Boot JDBC starter (managed via the parent if there is one) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- JDBI core/sqlobject support -->
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

<!-- MySQL driver & Flyway support -->
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <version>8.0.33</version>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
  <version>11.0.0</version>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
  <version>11.0.0</version>
</dependency>
</dependencies>
```

Example `application.yml` for a consuming Spring Boot service:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: myuser
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
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
