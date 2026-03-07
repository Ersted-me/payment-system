package com.ersted.personservice.entity;

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
@Table(schema = "person", name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @Column(name = "address", length = 128)
    private String address;

    @Column(name = "zip_code", length = 32)
    private String zipCode;

    @Column(name = "city", length = 32)
    private String city;

    @Column(name = "state", length = 32)
    private String state;

    @Column(name = "archived", nullable = false)
    private OffsetDateTime archived;

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

}
