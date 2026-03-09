package com.ersted.personservice.repository;

import com.ersted.personservice.entity.Individual;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndividualRepository extends JpaRepository<Individual, UUID> {

    @EntityGraph(attributePaths = {
            "user",
            "user.address",
            "user.address.country"
    })
    Optional<Individual> findWithDetailById(UUID id);


}
