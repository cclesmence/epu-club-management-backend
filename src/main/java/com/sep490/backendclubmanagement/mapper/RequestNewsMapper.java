package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.NewsRequestResponse;
import com.sep490.backendclubmanagement.entity.RequestNews;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestNewsMapper {

    @Mapping(source = "club.id",                 target = "clubId")
    @Mapping(source = "createdBy.id",            target = "createdByUserId")
    @Mapping(source = "news.id",                 target = "newsId")
    @Mapping(source = "createdBy.fullName",      target = "createdByFullName")
    @Mapping(source = "createdBy.email",         target = "createdByEmail")
    @Mapping(source = "createdBy.avatarUrl",     target = "createdByAvatarUrl")
    @Mapping(source = "createdBy.studentCode",   target = "createdByStudentCode")
    @Mapping(source = "club.clubName",           target = "clubName")
    @Mapping(source = "club.clubCode",           target = "clubCode")
    @Mapping(source = "club.logoUrl",            target = "clubLogoUrl")
    @Mapping(source = "team.id",                 target = "teamId")
    @Mapping(source = "team.teamName",           target = "teamName") // hoặc team.name tùy entity
    NewsRequestResponse toDto(RequestNews entity);

    @org.mapstruct.AfterMapping
    default void fillThumbAndType(RequestNews entity, @org.mapstruct.MappingTarget NewsRequestResponse dto) {
        if (dto.getThumbnailUrl() == null && entity.getNews() != null) {
            dto.setThumbnailUrl(entity.getNews().getThumbnailUrl());
        }
        if (dto.getNewsType() == null && entity.getNews() != null) {
            dto.setNewsType(entity.getNews().getNewsType());
        }
    }
}

