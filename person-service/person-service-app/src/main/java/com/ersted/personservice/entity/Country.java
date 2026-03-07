package com.ersted.personservice.entity;

import com.ersted.personservice.entity.status.CountryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(schema = "person", name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 32)
    private String name;

    @Column(name = "alpha2", length = 2)
    private String alpha2;

    @Column(name = "alpha3", length = 3)
    private String alpha3;

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private CountryStatus status;

}
