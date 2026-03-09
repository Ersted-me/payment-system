package com.ersted.personservice.service;

import com.ersted.personservice.entity.Country;
import com.ersted.personservice.entity.Individual;
import com.ersted.personservice.entity.status.IndividualStatus;
import com.ersted.personservice.exception.NotFoundException;
import com.ersted.personservice.exception.ValidateException;
import com.ersted.personservice.mapper.IndividualMapper;
import com.ersted.personservice.model.IndividualCreateProfileRequest;
import com.ersted.personservice.model.IndividualInfoResponse;
import com.ersted.personservice.model.IndividualInfoUpdateRequest;
import com.ersted.personservice.repository.CountryRepository;
import com.ersted.personservice.repository.IndividualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndividualService {

    private final IndividualRepository individualRepository;

    private final CountryRepository countryRepository;

    private final IndividualMapper individualMapper;

    @Transactional
    public IndividualInfoResponse create(IndividualCreateProfileRequest request) {
        log.info("Creating individual email: [{}]", request.getEmail());

        Country country = countryRepository.findCountryByAlpha3(request.getAddress().getCountry().getAlpha3())
                .orElseThrow(() ->{
                    log.warn("Country not found by alpha3: [{}]", request.getAddress().getCountry().getAlpha3());
                    return new ValidateException("Couldn't find the country by alpha3");
                });

        Individual individual = individualMapper.map(request);
        individual.setStatus(IndividualStatus.PENDING);
        individual.getUser().setFilled(true);
        individual.getUser().getAddress().setCountry(country);

        individualRepository.save(individual);

        log.info("Individual profile created, id: [{}]", individual.getId());
        return individualMapper.map(individual);
    }

    @Transactional(readOnly = true)
    public IndividualInfoResponse info(UUID userUuid) {
        return individualRepository.findWithDetailById(userUuid)
                .map(individualMapper::map)
                .orElseThrow(() -> {
                    log.warn("Couldn't find user with id: [{}]", userUuid);
                    return new NotFoundException("Couldn't find user with id: %s".formatted(userUuid));
                });
    }

    @Transactional
    public IndividualInfoResponse update(UUID userUuid, IndividualInfoUpdateRequest request) {

        Individual individual = individualRepository.findWithDetailById(userUuid)
                .orElseThrow(() -> new NotFoundException("Couldn't find user with id: %s".formatted(userUuid)));

        individualMapper.updateFromRequest(request, individual);

        if (request.getAddress() != null
                && request.getAddress().getCountry() != null
                && request.getAddress().getCountry().getAlpha3() != null) {

            Country country = countryRepository
                    .findCountryByAlpha3(request.getAddress().getCountry().getAlpha3())
                    .orElseThrow(() -> new ValidateException("Couldn't find the country by alpha3"));

            individual.getUser().getAddress().setCountry(country);
        }

        return individualMapper.map(individual);
    }

    @Transactional
    public void active(UUID userUuid) {
        Individual individual = individualRepository.findWithDetailById(userUuid)
                .orElseThrow(() -> new NotFoundException("Couldn't find user with id: %s".formatted(userUuid)));

        individual.setStatus(IndividualStatus.ACTIVE);
        individual.setVerifiedAt(OffsetDateTime.now());

        log.info("Individual activated, id: [{}]", userUuid);
    }

    @Transactional
    public void purge(UUID userUuid) {
        log.warn("Purging individual, id: [{}]", userUuid);
        individualRepository.deleteById(userUuid);
    }

    @Transactional
    public void archive(UUID userUuid) {
        Individual individual = individualRepository.findById(userUuid)
                .orElseThrow(() -> new NotFoundException("Couldn't find user with id: %s".formatted(userUuid)));

        individual.setStatus(IndividualStatus.ARCHIVED);
        individual.setArchivedAt(OffsetDateTime.now());

        log.info("Individual archived, id: [{}]", userUuid);
    }

}
