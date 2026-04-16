package com.ersted.walletservice.repository;

import com.ersted.walletservice.entity.OutboxMessage;
import com.ersted.walletservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByStatus(OutboxStatus status);

}
