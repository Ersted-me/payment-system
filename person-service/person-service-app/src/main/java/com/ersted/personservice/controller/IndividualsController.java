package com.ersted.personservice.controller;

import com.ersted.personservice.api.IndividualsApi;
import com.ersted.personservice.model.IndividualCreateProfileRequest;
import com.ersted.personservice.model.IndividualInfoResponse;
import com.ersted.personservice.model.IndividualInfoUpdateRequest;
import com.ersted.personservice.service.IndividualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class IndividualsController implements IndividualsApi {

    private final IndividualService individualService;

    @Override
    public ResponseEntity<IndividualInfoResponse> createIndividual(
            IndividualCreateProfileRequest individualCreateProfileRequest
    ) {
        var dto = individualService.create(individualCreateProfileRequest);
        return ResponseEntity
                .created(URI.create("/v1/individuals/" + dto.getId()))
                .body(dto);
    }

    @Override
    public ResponseEntity<Void> activateIndividual(UUID userUuid) {
        individualService.active(userUuid);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> archiveIndividual(UUID userUuid) {
        individualService.archive(userUuid);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<IndividualInfoResponse> getIndividual(UUID userUuid) {
        return ResponseEntity.ok(individualService.profileInfo(userUuid));
    }

    @Override
    public ResponseEntity<IndividualInfoResponse> updateIndividual(UUID userUuid, IndividualInfoUpdateRequest individualInfoUpdateRequest) {
        return ResponseEntity.ok(individualService.update(userUuid, individualInfoUpdateRequest));
    }

    @Override
    public ResponseEntity<Void> purgeIndividual(UUID userUuid) {
        individualService.purge(userUuid);
        return ResponseEntity.noContent().build();
    }

}
