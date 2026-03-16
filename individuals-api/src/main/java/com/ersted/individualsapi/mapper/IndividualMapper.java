package com.ersted.individualsapi.mapper;

import com.ersted.individualsapi.dto.UserRegistrationRequestProfile;
import com.ersted.personservice.sdk.model.IndividualCreateProfileRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IndividualMapper {

    IndividualCreateProfileRequest map(UserRegistrationRequestProfile profile);

}
