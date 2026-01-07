package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.request.AdminDepartmentUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.AdminDepartmentResponse;
import com.sep490.backendclubmanagement.dto.response.CampusSimpleResponse;
import com.sep490.backendclubmanagement.entity.AdminDepartment;
import com.sep490.backendclubmanagement.entity.Campus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminDepartmentMapper {

    // Entity -> DTO
    @Mapping(source = "campus", target = "campus")
    AdminDepartmentResponse toDTO(AdminDepartment department);

    List<AdminDepartmentResponse> toDTOs(List<AdminDepartment> departments);

    // Campus -> CampusSimpleResponse
    CampusSimpleResponse toCampusSimpleResponse(Campus campus);


    void updateEntityFromRequest(AdminDepartmentUpdateRequest request,
                                 @MappingTarget AdminDepartment department);
}