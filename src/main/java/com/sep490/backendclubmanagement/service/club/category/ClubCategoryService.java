package com.sep490.backendclubmanagement.service.club.category;

import com.sep490.backendclubmanagement.dto.request.CreateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.response.ClubCategoryDTO;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClubCategoryService {
    List<ClubCategoryDTO> getAllClubCategories();

    PageResponse<ClubCategoryDTO> getAllClubCategoriesWithFilter(String keyword, Pageable pageable) throws AppException;

    ClubCategoryDTO getClubCategoryById(Long id) throws AppException;

    ClubCategoryDTO createClubCategory(CreateClubCategoryRequest request) throws AppException;

    ClubCategoryDTO updateClubCategory(Long id, UpdateClubCategoryRequest request) throws AppException;

    void deleteClubCategory(Long id) throws AppException;
}

