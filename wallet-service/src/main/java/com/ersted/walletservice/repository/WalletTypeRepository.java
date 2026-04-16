package com.ersted.walletservice.repository;

import com.ersted.walletservice.entity.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTypeRepository extends JpaRepository<WalletType, UUID> {
}
