package com.zula.database.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zula.database")
public class DatabaseProperties {
    private boolean autoCreateSchema = true;
    private String schemaPrefix = "zula";
    private boolean enableAuditing = true;
    private boolean enableFlyway = true;
    private boolean autoCreateQueueSchema = true;
    private String queueSchemaSuffix = "queue";

    public boolean isAutoCreateSchema() { return autoCreateSchema; }
    public void setAutoCreateSchema(boolean autoCreateSchema) { this.autoCreateSchema = autoCreateSchema; }

    public String getSchemaPrefix() { return schemaPrefix; }
    public void setSchemaPrefix(String schemaPrefix) { this.schemaPrefix = schemaPrefix; }

    public boolean isEnableAuditing() { return enableAuditing; }
    public void setEnableAuditing(boolean enableAuditing) { this.enableAuditing = enableAuditing; }

    public boolean isEnableFlyway() { return enableFlyway; }
    public void setEnableFlyway(boolean enableFlyway) { this.enableFlyway = enableFlyway; }

    public boolean isAutoCreateQueueSchema() {
        return autoCreateQueueSchema;
    }

    public void setAutoCreateQueueSchema(boolean autoCreateQueueSchema) {
        this.autoCreateQueueSchema = autoCreateQueueSchema;
    }

    public String getQueueSchemaSuffix() {
        return queueSchemaSuffix;
    }

    public void setQueueSchemaSuffix(String queueSchemaSuffix) {
        this.queueSchemaSuffix = queueSchemaSuffix;
    }
}
