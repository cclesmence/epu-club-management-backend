package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.ClubCategoryDTO;
import com.sep490.backendclubmanagement.entity.club.ClubCategory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClubCategoryMapper {

    default ClubCategoryDTO toDTO(ClubCategory clubCategory) {
        if (clubCategory == null) return null;
        ClubCategoryDTO dto = new ClubCategoryDTO();
        dto.setId(clubCategory.getId());
        dto.setCategoryName(clubCategory.getCategoryName());
        dto.setClubCount(clubCategory.getClubs() == null ? 0 : clubCategory.getClubs().size());
        return dto;
    }

    List<ClubCategoryDTO> toDTOList(List<ClubCategory> clubCategories);
}
