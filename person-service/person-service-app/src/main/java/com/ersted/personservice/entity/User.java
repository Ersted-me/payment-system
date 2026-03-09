package com.ersted.personservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Audited
@Entity
@Table(schema = "person", name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "secret_key", length = 32)
    private String secretKey;

    @Column(name = "email", length = 1024)
    private String email;

    @Column(name = "first_name", length = 32)
    private String firstName;

    @Column(name = "last_name", length = 32)
    private String lastName;

    @Column(name = "filled")
    private Boolean filled;

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

}
