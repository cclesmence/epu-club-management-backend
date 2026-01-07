package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.LikeDTO;
import com.sep490.backendclubmanagement.entity.Like;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LikeMapper {

    @Mapping(target = "userId",   source = "user.id")
    @Mapping(target = "userName", source = "user.fullName")
    LikeDTO toDTO(Like like);
}
