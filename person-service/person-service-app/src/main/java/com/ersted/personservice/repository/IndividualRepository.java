package com.ersted.personservice.repository;

import com.ersted.personservice.entity.Individual;
import io.micrometer.observation.annotation.Observed;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndividualRepository extends JpaRepository<Individual, UUID> {

    @Observed(name = "individual.repository.findWithDetailById", contextualName = "repository.findWithDetailById")
    @EntityGraph(attributePaths = {
            "user",
            "user.address",
            "user.address.country"
    })
    Optional<Individual> findWithDetailById(UUID id);


}
