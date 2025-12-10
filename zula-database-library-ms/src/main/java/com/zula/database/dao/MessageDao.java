package com.zula.database.dao;

import com.zula.database.entity.MessageInbox;
import com.zula.database.entity.MessageOutbox;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.time.LocalDateTime;

@RegisterBeanMapper(MessageInbox.class)
@RegisterBeanMapper(MessageOutbox.class)
public interface MessageDao {

    @SqlUpdate("INSERT INTO <schema>.message_inbox(message_id, message_type, source_service, payload, status, processed_at, created_at, updated_at) VALUES (:messageId, :messageType, :sourceService, :payload, :status, :processedAt, :createdAt, :updatedAt)")
    void insertInbox(@BindBean MessageInbox message, @Define("schema") String schema);

    @SqlUpdate("UPDATE <schema>.message_inbox SET status = :status, processed_at = :processedAt, updated_at = :updatedAt WHERE message_id = :messageId")
    int updateInboxStatus(@Bind("messageId") String messageId,
                          @Bind("status") String status,
                          @Bind("processedAt") LocalDateTime processedAt,
                          @Bind("updatedAt") LocalDateTime updatedAt,
                          @Define("schema") String schema);

    @SqlQuery("SELECT id, message_id, message_type, source_service, payload, status, processed_at, created_at, updated_at FROM <schema>.message_inbox WHERE message_id = :messageId")
    MessageInbox findInboxByMessageId(@Bind("messageId") String messageId, @Define("schema") String schema);

    @SqlUpdate("INSERT INTO <schema>.message_outbox(message_id, message_type, target_service, payload, status, sent_at, retry_count, created_at, updated_at) VALUES (:messageId, :messageType, :targetService, :payload, :status, :sentAt, :retryCount, :createdAt, :updatedAt)")
    void insertOutbox(@BindBean MessageOutbox message, @Define("schema") String schema);

    @SqlQuery("SELECT id, message_id, message_type, target_service, payload, status, sent_at, retry_count, created_at, updated_at FROM <schema>.message_outbox WHERE message_id = :messageId")
    MessageOutbox findOutboxByMessageId(@Bind("messageId") String messageId, @Define("schema") String schema);

    @SqlQuery("SELECT id, message_id, message_type, target_service, payload, status, sent_at, retry_count, created_at, updated_at FROM <schema>.message_outbox")
    List<MessageOutbox> listOutbox(@Define("schema") String schema);
}

