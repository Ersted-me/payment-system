package com.ersted.personservice.mapper;

import com.ersted.personservice.entity.Individual;
import com.ersted.personservice.model.IndividualCreateProfileRequest;
import com.ersted.personservice.model.IndividualInfoResponse;
import com.ersted.personservice.model.IndividualInfoUpdateRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface IndividualMapper {

    @Mapping(source = "secretKey", target = "user.secretKey")
    @Mapping(source = "email", target = "user.email")
    @Mapping(source = "firstName", target = "user.firstName")
    @Mapping(source = "lastName", target = "user.lastName")
    @Mapping(source = "address", target = "user.address")
    Individual map(IndividualCreateProfileRequest dto);

    @Mapping(source = "individual.user.id", target = "userId")
    @Mapping(source = "individual.user.address", target = "address")
    @Mapping(source = "individual.user.created", target = "created")
    IndividualInfoResponse map(Individual individual);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "secretKey", target = "user.secretKey")
    @Mapping(source = "firstName", target = "user.firstName")
    @Mapping(source = "lastName", target = "user.lastName")
    @Mapping(source = "address", target = "user.address")
    @Mapping(target = "user.address.country", ignore = true)
    void updateFromRequest(IndividualInfoUpdateRequest request, @MappingTarget Individual individual);

}
