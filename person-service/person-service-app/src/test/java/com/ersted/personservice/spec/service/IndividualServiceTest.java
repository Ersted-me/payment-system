package com.ersted.personservice.spec.service;

import com.ersted.personservice.entity.Address;
import com.ersted.personservice.entity.Country;
import com.ersted.personservice.entity.Individual;
import com.ersted.personservice.entity.User;
import com.ersted.personservice.entity.status.IndividualStatus;
import com.ersted.personservice.exception.NotFoundException;
import com.ersted.personservice.exception.ValidateException;
import com.ersted.personservice.mapper.IndividualMapper;
import com.ersted.personservice.model.IndividualCreateProfileRequest;
import com.ersted.personservice.model.IndividualCreateProfileRequestAddress;
import com.ersted.personservice.model.IndividualCreateProfileRequestAddressCountry;
import com.ersted.personservice.model.IndividualInfoResponse;
import com.ersted.personservice.model.IndividualInfoUpdateRequest;
import com.ersted.personservice.repository.CountryRepository;
import com.ersted.personservice.repository.IndividualRepository;
import com.ersted.personservice.service.IndividualService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndividualServiceTest {

    @Mock
    private IndividualRepository individualRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private IndividualMapper individualMapper;

    @InjectMocks
    private IndividualService individualService;

    @Test
    void shouldCreateIndividualSuccessfully() {
        // Given
        IndividualCreateProfileRequest request = buildCreateRequest("RUS");
        Country country = buildCountry("Russia", "RU", "RUS");
        Individual individual = buildIndividual();
        IndividualInfoResponse expectedResponse = new IndividualInfoResponse();
        expectedResponse.setId(individual.getId());

        when(countryRepository.findCountryByAlpha3("RUS")).thenReturn(Optional.of(country));
        when(individualMapper.map(request)).thenReturn(individual);
        when(individualRepository.save(individual)).thenReturn(individual);
        when(individualMapper.map(individual)).thenReturn(expectedResponse);

        // When
        IndividualInfoResponse result = individualService.create(request);

        // Then
        assertNotNull(result);
        assertEquals(individual.getId(), result.getId());
        assertEquals(IndividualStatus.PENDING, individual.getStatus());
        assertTrue(individual.getUser().getFilled());
        assertEquals(country, individual.getUser().getAddress().getCountry());

        // Verify
        verify(countryRepository).findCountryByAlpha3("RUS");
        verify(individualRepository).save(individual);
    }

    @Test
    void shouldFailCreateWhenCountryNotFound() {
        // Given
        IndividualCreateProfileRequest request = buildCreateRequest("XYZ");

        when(countryRepository.findCountryByAlpha3("XYZ")).thenReturn(Optional.empty());

        // When / Then
        ValidateException exception = assertThrows(
                ValidateException.class,
                () -> individualService.create(request)
        );

        assertEquals("Couldn't find the country by alpha3", exception.getMessage());
        verifyNoInteractions(individualRepository, individualMapper);
    }

    @Test
    void shouldGetProfileInfoSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        Individual individual = buildIndividual();
        IndividualInfoResponse expectedResponse = new IndividualInfoResponse();
        expectedResponse.setId(userUuid);

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.of(individual));
        when(individualMapper.map(individual)).thenReturn(expectedResponse);

        // When
        IndividualInfoResponse result = individualService.profileInfo(userUuid);

        // Then
        assertNotNull(result);
        assertEquals(userUuid, result.getId());

        // Verify
        verify(individualRepository).findWithDetailById(userUuid);
    }

    @Test
    void shouldFailGetProfileInfoWhenNotFound() {
        // Given
        UUID userUuid = UUID.randomUUID();

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.empty());

        // When / Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> individualService.profileInfo(userUuid)
        );

        assertTrue(exception.getMessage().contains(userUuid.toString()));
    }

    @Test
    void shouldUpdateIndividualSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        IndividualInfoUpdateRequest updateRequest = new IndividualInfoUpdateRequest();
        Individual individual = buildIndividual();
        IndividualInfoResponse expectedResponse = new IndividualInfoResponse();
        expectedResponse.setId(userUuid);

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.of(individual));
        doNothing().when(individualMapper).updateFromRequest(updateRequest, individual);
        when(individualMapper.map(individual)).thenReturn(expectedResponse);

        // When
        IndividualInfoResponse result = individualService.update(userUuid, updateRequest);

        // Then
        assertNotNull(result);
        verify(individualMapper).updateFromRequest(updateRequest, individual);
        verify(individualMapper).map(individual);
    }

    @Test
    void shouldFailUpdateWhenIndividualNotFound() {
        // Given
        UUID userUuid = UUID.randomUUID();
        IndividualInfoUpdateRequest updateRequest = new IndividualInfoUpdateRequest();

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.empty());

        // When / Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> individualService.update(userUuid, updateRequest)
        );

        assertTrue(exception.getMessage().contains(userUuid.toString()));
    }

    @Test
    void shouldActivateIndividualSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        Individual individual = buildIndividual();

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.of(individual));

        // When
        individualService.active(userUuid);

        // Then
        assertEquals(IndividualStatus.ACTIVE, individual.getStatus());
        assertNotNull(individual.getVerifiedAt());
    }

    @Test
    void shouldFailActivateWhenIndividualNotFound() {
        // Given
        UUID userUuid = UUID.randomUUID();

        when(individualRepository.findWithDetailById(userUuid)).thenReturn(Optional.empty());

        // When / Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> individualService.active(userUuid)
        );

        assertTrue(exception.getMessage().contains(userUuid.toString()));
    }

    @Test
    void shouldArchiveIndividualSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        Individual individual = buildIndividual();

        when(individualRepository.findById(userUuid)).thenReturn(Optional.of(individual));

        // When
        individualService.archive(userUuid);

        // Then
        assertEquals(IndividualStatus.ARCHIVED, individual.getStatus());
        assertNotNull(individual.getArchivedAt());
    }

    @Test
    void shouldFailArchiveWhenIndividualNotFound() {
        // Given
        UUID userUuid = UUID.randomUUID();

        when(individualRepository.findById(userUuid)).thenReturn(Optional.empty());

        // When / Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> individualService.archive(userUuid)
        );

        assertTrue(exception.getMessage().contains(userUuid.toString()));
    }

    @Test
    void shouldPurgeIndividualSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        doNothing().when(individualRepository).deleteById(userUuid);

        // When
        individualService.purge(userUuid);

        // Verify
        verify(individualRepository).deleteById(userUuid);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private IndividualCreateProfileRequest buildCreateRequest(String alpha3) {
        IndividualCreateProfileRequestAddressCountry country =
                new IndividualCreateProfileRequestAddressCountry("Russia", "RU", alpha3);
        IndividualCreateProfileRequestAddress address =
                new IndividualCreateProfileRequestAddress("Test Street 1", "123456", "Moscow", "Moscow", country);
        return new IndividualCreateProfileRequest("AB123456", "+79001234567", "test@test.com", "John", "Doe", "secret123", address);
    }

    private Country buildCountry(String name, String alpha2, String alpha3) {
        Country country = new Country();
        country.setName(name);
        country.setAlpha2(alpha2);
        country.setAlpha3(alpha3);
        return country;
    }

    private Individual buildIndividual() {
        Address address = new Address();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setFilled(false);
        user.setAddress(address);

        Individual individual = new Individual();
        individual.setId(UUID.randomUUID());
        individual.setPassportNumber("AB123456");
        individual.setPhoneNumber("+79001234567");
        individual.setStatus(IndividualStatus.PENDING);
        individual.setUser(user);
        return individual;
    }

}
