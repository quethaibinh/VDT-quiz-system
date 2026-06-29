package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepo extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            select e from OutboxEvent e
            where e.status in :statuses
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            order by e.createdAt asc
            """)
    List<OutboxEvent> findClaimable(
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select e from OutboxEvent e
            where e.status = com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus.PUBLISHED
              and e.eventType = :eventType
              and e.publishedAt is not null
              and e.publishedAt <= :publishedBefore
            order by e.publishedAt asc
            """)
    List<OutboxEvent> findPublishedForReconciliation(
            @Param("eventType") String eventType,
            @Param("publishedBefore") LocalDateTime publishedBefore,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update OutboxEvent e
            set e.status = com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus.PROCESSING,
                e.nextRetryAt = :leaseUntil
            where e.id = :id
              and e.status in :statuses
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            """)
    // Ganh quyen xu ly outbox trong mot lease ngan de tranh nhieu worker publish trung event.
    int claim(
            @Param("id") UUID id,
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );
}
