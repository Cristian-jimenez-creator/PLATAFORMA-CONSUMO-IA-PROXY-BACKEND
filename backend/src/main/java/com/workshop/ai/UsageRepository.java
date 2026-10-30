package com.workshop.ai;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(String userId, Instant since);
}
