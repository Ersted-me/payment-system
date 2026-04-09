package com.ersted.walletservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "wallet", name = "wallet_types")
public class WalletType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private UUID uuid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private OffsetDateTime updated;

    @Column(name = "name", nullable = false, length = 32)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 18)
    private WalletTypeStatus status;

    @Column(name = "archived_at")
    private OffsetDateTime archived;

    @Column(name = "user_type", length = 15)
    private String userType;

    @Column(name = "creator", length = 255)
    private String creator;

    @Column(name = "modifier", length = 255)
    private String modifier;

}
