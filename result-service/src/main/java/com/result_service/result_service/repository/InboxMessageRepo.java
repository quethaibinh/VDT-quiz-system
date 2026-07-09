package com.result_service.result_service.repository;

import com.result_service.result_service.model.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxMessageRepo extends JpaRepository<InboxMessage, UUID> {
}
