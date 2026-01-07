package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.OutcomeTransactionResponse;
import com.sep490.backendclubmanagement.entity.OutcomeTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OutcomeTransactionMapper {

    @Mapping(source = "createdBy.fullName", target = "createdByName")
    OutcomeTransactionResponse toResponse(OutcomeTransaction outcomeTransaction);
}

