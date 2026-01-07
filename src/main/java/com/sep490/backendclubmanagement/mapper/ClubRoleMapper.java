package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.ClubRoleResponse;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ClubRoleMapper {
    @Mapping(source = "systemRole.id", target = "systemRoleId")
    @Mapping(source = "systemRole.roleName", target = "systemRoleName")
    ClubRoleResponse toDto(ClubRole role);
    List<ClubRoleResponse> toDtos(List<ClubRole> roles);
}









