package com.zula.database.entity;

import com.zula.database.core.BaseEntity;

import java.time.LocalDateTime;

/**
 * POJO representation of the message_outbox table for use with JDBI.
 */
public class MessageOutbox extends BaseEntity {

    private String messageId;

    private String messageType;

    private String targetService;

    private String payload;

    private String status;

    private LocalDateTime sentAt;

    private Integer retryCount = 0;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getTargetService() { return targetService; }
    public void setTargetService(String targetService) { this.targetService = targetService; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}
