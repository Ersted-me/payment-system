package com.ersted.personservice.entity;

import com.ersted.personservice.entity.status.IndividualStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Audited
@Entity
@Table(schema = "person", name = "individuals")
public class Individual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "passport_number", length = 32)
    private String passportNumber;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IndividualStatus status;

}
