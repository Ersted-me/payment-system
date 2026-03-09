package com.ersted.personservice.repository;

import com.ersted.personservice.entity.Country;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {

    Optional<Country> findCountryByAlpha3(@NotNull @Size(min = 3, max = 3) String alpha3);

}
