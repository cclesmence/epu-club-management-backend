package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.ClubDetailData;
import com.sep490.backendclubmanagement.dto.response.ClubManagementResponse;
import com.sep490.backendclubmanagement.entity.club.Club;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClubMapper {

    @Mapping(source = "campus.id", target = "campusId")
    @Mapping(source = "campus.campusName", target = "campusName")
    @Mapping(source = "campus.campusCode", target = "campusCode")
    @Mapping(source = "clubCategory.id", target = "categoryId")
    @Mapping(source = "clubCategory.categoryName", target = "categoryName")
    @Mapping(target = "totalMembers", ignore = true)
    @Mapping(target = "totalEvents", ignore = true)
    @Mapping(target = "totalNews", ignore = true)
    @Mapping(target = "isRecruiting", ignore = true)
    @Mapping(target = "presidents", ignore = true)
    ClubDetailData toClubDetailData(Club club);

//    @Mapping(source = "user.fullName", target = "fullName")
//    @Mapping(source = "user.email", target = "email")
//    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
//    ClubPresidentData toPresidentData(ClubMemberShip membership);

    @Mapping(source = "campus.campusName", target = "campusName")
    @Mapping(source = "campus.id", target = "campusId")
    @Mapping(source = "clubCategory.categoryName", target = "categoryName")
    @Mapping(source = "clubCategory.id", target = "categoryId")
    @Mapping(target = "totalMembers", ignore = true)
    @Mapping(target = "totalEvents", ignore = true)
    @Mapping(target = "totalPosts", ignore = true)
    @Mapping(target = "presidents", ignore = true)
    ClubManagementResponse toClubManagementResponse(Club club);

    // Note: Statistics (totalMembers, totalEvents, totalPosts) and presidents list
    // are set in ClubService using dedicated queries to avoid Cartesian product issues
}

