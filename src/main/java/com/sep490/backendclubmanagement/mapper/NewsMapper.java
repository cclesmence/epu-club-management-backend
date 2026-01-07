package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.entity.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface NewsMapper {

    // club info
    @Mapping(source = "club.id",       target = "clubId")
    @Mapping(source = "club.clubName", target = "clubName")

    // draft flag: entity.isDraft -> dto.draft
    @Mapping(source = "isDraft",       target = "draft")

    // author info từ createdBy
    @Mapping(source = "createdBy.id",        target = "authorId")
    @Mapping(source = "createdBy.fullName",  target = "authorName")
    @Mapping(source = "createdBy.email",     target = "authorEmail")

    // authorRole: nếu không gắn CLB => STAFF, ngược lại => CLUB
    @Mapping(target = "authorRole",
            expression = "java(news.getClub() == null ? \"STAFF\" : \"CLUB\")")

    @Mapping(source = "hidden",  target = "hidden")
    @Mapping(source = "deleted", target = "deleted")
    // format updatedAt
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "toStringTime")
    NewsData toDto(News news);

    @Named("toStringTime")
    static String toStringTime(LocalDateTime time) {
        if (time == null) return null;
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
