package com.workshop.ai;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usage_records", indexes = @Index(name = "idx_usage_user_time", columnList = "user_id,created_at"))
public class UsageRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private long tokens;
    private Instant createdAt;

    protected UsageRecord() {}
    UsageRecord(String userId, long tokens) {
        this.userId = userId;
        this.tokens = tokens;
        this.createdAt = Instant.now();
    }
    public String getUserId() { return userId; }
    public long getTokens() { return tokens; }
    public Instant getCreatedAt() { return createdAt; }
}
