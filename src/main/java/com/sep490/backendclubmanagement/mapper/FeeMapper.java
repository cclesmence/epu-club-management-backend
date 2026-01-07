package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.FeeDetailResponse;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FeeMapper {
    FeeMapper INSTANCE = Mappers.getMapper(FeeMapper.class);

    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "semesterName", source = "semester.semesterName")
    FeeDetailResponse toFeeDetailResponse(Fee fee);


}
