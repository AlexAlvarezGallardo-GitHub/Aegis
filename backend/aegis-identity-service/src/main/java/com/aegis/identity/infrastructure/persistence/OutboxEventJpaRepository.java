package com.aegis.identity.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
