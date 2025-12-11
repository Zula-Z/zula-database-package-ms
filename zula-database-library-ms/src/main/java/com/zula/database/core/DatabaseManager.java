package com.zula.database.core;

import com.zula.database.config.DatabaseProperties;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
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
            handle.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schema));

            handle.execute("CREATE TABLE IF NOT EXISTS " + outboxTable + " (" +
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
                    ") ENGINE=InnoDB");

            handle.execute("CREATE TABLE IF NOT EXISTS " + inboxTable + " (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "message_id VARCHAR(255) NOT NULL UNIQUE," +
                    "message_type VARCHAR(100) NOT NULL," +
                    "source_service VARCHAR(100) NOT NULL," +
                    "payload TEXT NOT NULL," +
                    "status VARCHAR(50) NOT NULL," +
                    "processed_at DATETIME NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB");

            try {
                handle.execute("CREATE INDEX " + outboxIndex + " ON " + outboxTable + " (status)");
            } catch (Exception ignored) {
            }
            try {
                handle.execute("CREATE INDEX " + inboxIndex + " ON " + inboxTable + " (status)");
            } catch (Exception ignored) {
            }
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

}
