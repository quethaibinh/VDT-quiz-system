package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
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

    @Modifying
    @Query("""
            update OutboxEvent e
            set e.status = com.exam_service.exam_service.model.entity.enums.OutboxStatus.PROCESSING,
                e.nextRetryAt = :leaseUntil
            where e.id = :id
              and e.status in :statuses
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            """)
    // gianh quyen xu ly, cho phep 1 worker instance doc chiem va xu ly outbox event trong 1 khoang thoi gian
    // de khong bi xu ly lap khi worker quet bang outbox
    int claim(
            @Param("id") UUID id,
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );
}
