package com.ersted.walletservice.repository;

import com.ersted.walletservice.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, UUID> {
}
