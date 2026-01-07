package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.dto.response.StaffSummaryResponse;
import com.sep490.backendclubmanagement.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    StaffSummaryResponse toStaffSummary(User user);
    List<StaffSummaryResponse> toStaffSummaries(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UpdateUserProfileRequest request, @MappingTarget User user);
}


