package com.zula.database.core;

import com.zula.database.config.DatabaseProperties;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

public class DatabaseManager {

    private final DatabaseProperties properties;
    private final Jdbi jdbi;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Autowired
    public DatabaseManager(DatabaseProperties properties, Jdbi jdbi) {
        this.properties = properties;
        this.jdbi = jdbi;
    }

    @PostConstruct
    public void init() {
        System.out.println("Zula Database Manager initialized");
        System.out.println("Auto-create schema: " + properties.isAutoCreateSchema());
        System.out.println("Service schema: " + generateSchemaName());
        if (properties.isAutoCreateQueueSchema()) {
            createQueueSchemaAndTables();
        }
    }

    public String generateSchemaName() {
        String prefix = properties.getSchemaPrefix();
        return (prefix.isEmpty() ? "" : prefix + "_") + serviceName.toLowerCase();
    }

    /**
     * Queue schema is always based on the service name with a fixed suffix so each service
     * gets its own logical inbox/outbox tables.
     */
    public String generateQueueSchemaName() {
        String suffix = properties.getQueueSchemaSuffix();
        String normalizedService = normalizeName(serviceName);
        return normalizedService + "_" + (suffix == null ? "queue" : normalizeName(suffix));
    }

    public String generateTableName(String tableName) {
        return generateSchemaName() + "." + tableName.toLowerCase();
    }

    public void createQueueSchemaAndTables() {
        String schema = generateQueueSchemaName();
        String outboxTable = schema + ".message_outbox";
        String inboxTable = schema + ".message_inbox";
        String outboxIndex = schema + "_idx_outbox_status";
        String inboxIndex = schema + "_idx_inbox_status";

        jdbi.useHandle(handle -> {
            QueueDialect dialect = QueueDialect.detect(handle);
            handle.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schema));

            handle.execute(dialect.outboxTableSql(outboxTable));
            handle.execute(dialect.inboxTableSql(inboxTable));

            executeIndex(handle, dialect, outboxIndex, outboxTable);
            executeIndex(handle, dialect, inboxIndex, inboxTable);
        });

        System.out.println("Zula Database Manager ensured queue schema exists: " + schema + " at " + LocalDateTime.now());
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return "unknown_service";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9_]", "_");
    }

    private void executeIndex(Handle handle, QueueDialect dialect, String indexName, String tableName) {
        try {
            handle.execute(dialect.indexSql(indexName, tableName));
        } catch (Exception ignored) {
            // Some platforms throw when the index already exists; ignore duplicate-index failures.
        }
    }

    /**
     * Database-specific queue-related statements so we can reuse this library across PostgreSQL and MySQL.
     */
    private enum QueueDialect {
        MYSQL {
            @Override
            String outboxTableSql(String table) {
                return "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "message_id VARCHAR(255) NOT NULL UNIQUE," +
                        "message_type VARCHAR(100) NOT NULL," +
                        "target_service VARCHAR(100) NOT NULL," +
                        "payload TEXT NOT NULL," +
                        "status VARCHAR(50) NOT NULL," +
                        "sent_at DATETIME NULL," +
                        "retry_count INT DEFAULT 0," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB";
            }

            @Override
            String inboxTableSql(String table) {
                return "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "message_id VARCHAR(255) NOT NULL UNIQUE," +
                        "message_type VARCHAR(100) NOT NULL," +
                        "source_service VARCHAR(100) NOT NULL," +
                        "payload TEXT NOT NULL," +
                        "status VARCHAR(50) NOT NULL," +
                        "processed_at DATETIME NULL," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB";
            }

            @Override
            String indexSql(String indexName, String tableName) {
                return "CREATE INDEX " + indexName + " ON " + tableName + " (status)";
            }
        },
        POSTGRES {
            @Override
            String outboxTableSql(String table) {
                return "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "message_id VARCHAR(255) NOT NULL UNIQUE," +
                        "message_type VARCHAR(100) NOT NULL," +
                        "target_service VARCHAR(100) NOT NULL," +
                        "payload TEXT NOT NULL," +
                        "status VARCHAR(50) NOT NULL," +
                        "sent_at TIMESTAMP," +
                        "retry_count INTEGER DEFAULT 0," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
            }

            @Override
            String inboxTableSql(String table) {
                return "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "message_id VARCHAR(255) NOT NULL UNIQUE," +
                        "message_type VARCHAR(100) NOT NULL," +
                        "source_service VARCHAR(100) NOT NULL," +
                        "payload TEXT NOT NULL," +
                        "status VARCHAR(50) NOT NULL," +
                        "processed_at TIMESTAMP," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
            }

            @Override
            String indexSql(String indexName, String tableName) {
                return "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (status)";
            }
        };

        abstract String outboxTableSql(String table);
        abstract String inboxTableSql(String table);
        abstract String indexSql(String indexName, String tableName);

        static QueueDialect detect(Handle handle) {
            try {
                String productName = handle.getConnection().getMetaData().getDatabaseProductName();
                if (productName == null) {
                    return POSTGRES;
                }
                String normalized = productName.toLowerCase(Locale.ROOT);
                if (normalized.contains("mysql") || normalized.contains("maria")) {
                    return MYSQL;
                }
                if (normalized.contains("postgres") || normalized.contains("pgsql")) {
                    return POSTGRES;
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to determine database vendor for queue schema creation", ex);
            }
            return POSTGRES;
        }
    }
}
