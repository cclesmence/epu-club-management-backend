package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.IncomeTransactionResponse;
import com.sep490.backendclubmanagement.entity.IncomeTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncomeTransactionMapper {

    @Mapping(source = "fee.id", target = "feeId")
    @Mapping(source = "fee.title", target = "feeTitle")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "createdBy.fullName", target = "createdByName")
    IncomeTransactionResponse toResponse(IncomeTransaction incomeTransaction);
}

