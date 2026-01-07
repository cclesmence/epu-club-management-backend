package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.TeamResponse;
import com.sep490.backendclubmanagement.entity.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeamMapper {
    @Mapping(source = "teamName", target = "teamName")
    TeamResponse toDto(Team team);
}
