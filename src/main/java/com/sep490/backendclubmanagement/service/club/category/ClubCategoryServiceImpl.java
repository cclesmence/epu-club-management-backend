package com.sep490.backendclubmanagement.service.club.category;

import com.sep490.backendclubmanagement.dto.request.CreateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.response.ClubCategoryDTO;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.club.ClubCategory;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.ClubCategoryMapper;
import com.sep490.backendclubmanagement.repository.ClubCategoryRepository;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubCategoryServiceImpl implements ClubCategoryService {

    private final ClubCategoryRepository clubCategoryRepository;
    private final ClubCategoryMapper clubCategoryMapper;
    private final RoleService roleService;

    @Override
    @Transactional(readOnly = true)
    public List<ClubCategoryDTO> getAllClubCategories() {
        List<ClubCategory> categories = clubCategoryRepository.findAll();
        return clubCategoryMapper.toDTOList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClubCategoryDTO> getAllClubCategoriesWithFilter(String keyword, Pageable pageable) throws AppException {

        Page<ClubCategory> categoryPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all categories without keyword filter
            categoryPage = clubCategoryRepository.findAllWithFilter(
                null,
                PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization
            List<ClubCategory> filteredList = categoryPage.getContent().stream()
                    .filter(category -> {
                        String categoryName = normalizeVietnamese(category.getCategoryName() != null ? category.getCategoryName() : "");

                        // Split keyword into individual words for better matching
                        String[] keywords = trimmedKeyword.split("\\s+");
                        for (String kw : keywords) {
                            String normalizedKw = normalizeVietnamese(kw);
                            if (categoryName.contains(normalizedKw)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<ClubCategory> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            categoryPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            categoryPage = clubCategoryRepository.findAllWithFilter(
                keyword,
                pageable
            );
        }

        Page<ClubCategoryDTO> dtoPage = categoryPage.map(clubCategoryMapper::toDTO);

        // Convert to 1-based pagination
        return PageResponse.<ClubCategoryDTO>builder()
                .content(dtoPage.getContent())
                .pageNumber(dtoPage.getNumber() + 1) // Convert from 0-based to 1-based
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .hasNext(dtoPage.hasNext())
                .hasPrevious(dtoPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClubCategoryDTO getClubCategoryById(Long id) throws AppException {
        ClubCategory category = clubCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại câu lạc bộ với ID: " + id));
        return clubCategoryMapper.toDTO(category);
    }

    @Override
    @Transactional
    public ClubCategoryDTO createClubCategory(CreateClubCategoryRequest request) throws AppException {

        // Kiểm tra tên thể loại đã tồn tại chưa
        if (clubCategoryRepository.existsByCategoryNameIgnoreCase(request.getCategoryName())) {
            throw new AppException(ErrorCode.CLUB_CATEGORY_ALREADY_EXISTS);
        }

        ClubCategory category = ClubCategory.builder()
            .categoryName(request.getCategoryName().trim())
            .build();

        ClubCategory savedCategory = clubCategoryRepository.save(category);
        log.info("Created club category with ID: {}", savedCategory.getId());

        return clubCategoryMapper.toDTO(savedCategory);
    }

    @Override
    @Transactional
    public ClubCategoryDTO updateClubCategory(Long id, UpdateClubCategoryRequest request) throws AppException {

        ClubCategory category = clubCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại câu lạc bộ với ID: " + id));

        // Kiểm tra tên thể loại mới có trùng với thể loại khác không
        if (clubCategoryRepository.existsByCategoryNameIgnoreCaseAndIdNot(request.getCategoryName(), id)) {
            throw new AppException(ErrorCode.CLUB_CATEGORY_ALREADY_EXISTS);
        }

        category.setCategoryName(request.getCategoryName().trim());
        ClubCategory updatedCategory = clubCategoryRepository.save(category);
        log.info("Updated club category with ID: {}", id);

        return clubCategoryMapper.toDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteClubCategory(Long id) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ STAFF mới có quyền xóa thể loại câu lạc bộ");
        }

        ClubCategory category = clubCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại câu lạc bộ với ID: " + id));

        // Kiểm tra xem có câu lạc bộ nào đang sử dụng thể loại này không
        if (category.getClubs() != null && !category.getClubs().isEmpty()) {
            throw new AppException(ErrorCode.CLUB_CATEGORY_IN_USE,
                "Không thể xóa thể loại này vì đang có " + category.getClubs().size() + " câu lạc bộ sử dụng");
        }

        clubCategoryRepository.delete(category);
        log.info("Deleted club category with ID: {}", id);
    }

    private String normalizeVietnamese(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("đ", "d").replace("Đ", "d");
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }
}
