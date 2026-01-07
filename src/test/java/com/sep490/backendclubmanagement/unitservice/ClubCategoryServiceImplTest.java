package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubCategoryRequest;
import com.sep490.backendclubmanagement.dto.response.ClubCategoryDTO;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubCategory;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.ClubCategoryMapper;
import com.sep490.backendclubmanagement.repository.ClubCategoryRepository;
import com.sep490.backendclubmanagement.service.club.category.ClubCategoryServiceImpl;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ClubCategoryServiceImplTest {

    @Mock
    private ClubCategoryRepository clubCategoryRepository;

    @Mock
    private ClubCategoryMapper clubCategoryMapper;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ClubCategoryServiceImpl clubCategoryService;

    private ClubCategory testCategory;
    private ClubCategoryDTO testCategoryDTO;
    private final Long testUserId = 1L;
    private final Long testCategoryId = 1L;
    private final String testCategoryName = "Thể thao";

    @BeforeEach
    void setup() {
        // Setup test category
        testCategory = ClubCategory.builder()
                .id(testCategoryId)
                .categoryName(testCategoryName)
                .clubs(new HashSet<>())
                .build();

        // Setup test category DTO
        testCategoryDTO = new ClubCategoryDTO();
        testCategoryDTO.setId(testCategoryId);
        testCategoryDTO.setCategoryName(testCategoryName);
    }

    // ================= getAllClubCategories Tests =================

    @Test
    void getAllClubCategories_success_returnsList() {
        // Arrange
        List<ClubCategory> categories = List.of(testCategory);
        List<ClubCategoryDTO> categoryDTOs = List.of(testCategoryDTO);

        when(clubCategoryRepository.findAll()).thenReturn(categories);
        when(clubCategoryMapper.toDTOList(categories)).thenReturn(categoryDTOs);

        // Act
        List<ClubCategoryDTO> result = clubCategoryService.getAllClubCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
//        assertEquals(testCategoryName, result.getFirst().getCategoryName());

        verify(clubCategoryRepository, times(1)).findAll();
        verify(clubCategoryMapper, times(1)).toDTOList(categories);
    }

    @Test
    void getAllClubCategories_emptyList_returnsEmptyList() {
        // Arrange
        when(clubCategoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(clubCategoryMapper.toDTOList(anyList())).thenReturn(Collections.emptyList());

        // Act
        List<ClubCategoryDTO> result = clubCategoryService.getAllClubCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(clubCategoryRepository, times(1)).findAll();
    }

    // ================= getAllClubCategoriesWithFilter Tests =================

    @Test
    void getAllClubCategoriesWithFilter_noKeyword_returnsPageWith1BasedPagination() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10); // Request page 0
        Page<ClubCategory> categoryPage = new PageImpl<>(List.of(testCategory), pageable, 1);

        when(clubCategoryRepository.findAllWithFilter(null, pageable)).thenReturn(categoryPage);
        when(clubCategoryMapper.toDTO(testCategory)).thenReturn(testCategoryDTO);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
//        assertEquals(testCategoryName, result.getContent().getFirst().getCategoryName());

        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber(), "Page 0 should become page 1 (1-based)");
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());

        verify(clubCategoryRepository, times(1)).findAllWithFilter(null, pageable);
    }

    @Test
    void getAllClubCategoriesWithFilter_page1_returnsPageNumber2() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(1, 10); // Request page 1 (0-based)
        Page<ClubCategory> categoryPage = new PageImpl<>(List.of(testCategory), pageable, 25);

        when(clubCategoryRepository.findAllWithFilter(null, pageable)).thenReturn(categoryPage);
        when(clubCategoryMapper.toDTO(testCategory)).thenReturn(testCategoryDTO);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getPageNumber(), "Page 1 should become page 2 (1-based)");
        assertTrue(result.isHasPrevious(), "Page 2 should have previous");
        assertTrue(result.isHasNext(), "Should have next page with 25 total elements");
        assertEquals(3, result.getTotalPages(), "Should have 3 total pages (25 elements / 10 per page)");
    }

    @Test
    void getAllClubCategoriesWithFilter_withKeyword_returnsFilteredPage() throws AppException {
        // Arrange
        String keyword = "the thao";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClubCategory> allCategories = new PageImpl<>(List.of(testCategory), PageRequest.of(0, Integer.MAX_VALUE), 1);

        when(clubCategoryRepository.findAllWithFilter(isNull(), any(PageRequest.class))).thenReturn(allCategories);
        when(clubCategoryMapper.toDTO(testCategory)).thenReturn(testCategoryDTO);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(keyword, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size(), "Should contain 1 category matching normalized keyword");
//        assertEquals(testCategoryName, result.getContent().getFirst().getCategoryName());
        assertEquals(1, result.getPageNumber(), "Should return 1-based page number");
        assertEquals(1, result.getTotalElements());

        verify(clubCategoryRepository, times(1)).findAllWithFilter(isNull(), any(PageRequest.class));
        verify(clubCategoryMapper, times(1)).toDTO(testCategory);
    }

    @Test
    void getAllClubCategoriesWithFilter_withEmptyKeyword_usesRepositoryFilter() throws AppException {
        // Arrange
        String keyword = "   ";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClubCategory> categoryPage = new PageImpl<>(List.of(testCategory), pageable, 1);

        when(clubCategoryRepository.findAllWithFilter(keyword, pageable)).thenReturn(categoryPage);
        when(clubCategoryMapper.toDTO(testCategory)).thenReturn(testCategoryDTO);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(keyword, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getPageNumber());

        verify(clubCategoryRepository, times(1)).findAllWithFilter(keyword, pageable);
    }

    @Test
    void getAllClubCategoriesWithFilter_keywordNotMatching_returnsEmptyPage() throws AppException {
        // Arrange
        String keyword = "khoa hoc";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClubCategory> allCategories = new PageImpl<>(List.of(testCategory), PageRequest.of(0, Integer.MAX_VALUE), 1);

        when(clubCategoryRepository.findAllWithFilter(isNull(), any(PageRequest.class))).thenReturn(allCategories);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(keyword, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size(), "Should return empty list when keyword doesn't match");
        assertEquals(0, result.getTotalElements());

        verify(clubCategoryRepository, times(1)).findAllWithFilter(isNull(), any(PageRequest.class));
        verify(clubCategoryMapper, never()).toDTO(any());
    }

    @Test
    void getAllClubCategoriesWithFilter_withKeywordMultipleCategories_returnsFilteredResults() throws AppException {
        // Arrange
        String keyword = "thao";
        Pageable pageable = PageRequest.of(0, 10);

        ClubCategory category1 = ClubCategory.builder().id(1L).categoryName("Thể thao").build();
        ClubCategory category2 = ClubCategory.builder().id(2L).categoryName("Văn hóa").build();
        ClubCategory category3 = ClubCategory.builder().id(3L).categoryName("Thể thao điện tử").build();

        ClubCategoryDTO dto1 = new ClubCategoryDTO();
        dto1.setId(1L);
        dto1.setCategoryName("Thể thao");

        ClubCategoryDTO dto3 = new ClubCategoryDTO();
        dto3.setId(3L);
        dto3.setCategoryName("Thể thao điện tử");

        Page<ClubCategory> allCategories = new PageImpl<>(
            List.of(category1, category2, category3),
            PageRequest.of(0, Integer.MAX_VALUE),
            3
        );

        when(clubCategoryRepository.findAllWithFilter(isNull(), any(PageRequest.class))).thenReturn(allCategories);
        when(clubCategoryMapper.toDTO(category1)).thenReturn(dto1);
        when(clubCategoryMapper.toDTO(category3)).thenReturn(dto3);

        // Act
        PageResponse<ClubCategoryDTO> result = clubCategoryService.getAllClubCategoriesWithFilter(keyword, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size(), "Should return 2 categories containing 'thao'");
        assertEquals(2, result.getTotalElements());

        verify(clubCategoryRepository, times(1)).findAllWithFilter(isNull(), any(PageRequest.class));
    }

    // ================= getClubCategoryById Tests =================

    @Test
    void getClubCategoryById_existingId_returnsCategory() throws AppException {
        // Arrange
        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(clubCategoryMapper.toDTO(testCategory)).thenReturn(testCategoryDTO);

        // Act
        ClubCategoryDTO result = clubCategoryService.getClubCategoryById(testCategoryId);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryId, result.getId());
        assertEquals(testCategoryName, result.getCategoryName());

        verify(clubCategoryRepository, times(1)).findById(testCategoryId);
        verify(clubCategoryMapper, times(1)).toDTO(testCategory);
    }

    @Test
    void getClubCategoryById_nonExistingId_throwsNotFoundException() {
        // Arrange
        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                clubCategoryService.getClubCategoryById(testCategoryId));
        assertTrue(exception.getMessage().contains("Không tìm thấy thể loại câu lạc bộ"));

        verify(clubCategoryRepository, times(1)).findById(testCategoryId);
        verify(clubCategoryMapper, never()).toDTO(any());
    }

    // ================= createClubCategory Tests =================

    @Test
    void createClubCategory_validRequest_returnsCreatedCategory() throws AppException {
        // Arrange
        CreateClubCategoryRequest request = new CreateClubCategoryRequest();
        request.setCategoryName("  Văn hóa  ");

        ClubCategory newCategory = ClubCategory.builder()
                .id(2L)
                .categoryName("Văn hóa")
                .build();

        ClubCategoryDTO newCategoryDTO = new ClubCategoryDTO();
        newCategoryDTO.setId(2L);
        newCategoryDTO.setCategoryName("Văn hóa");

        when(clubCategoryRepository.existsByCategoryNameIgnoreCase("  Văn hóa  ")).thenReturn(false);
        when(clubCategoryRepository.save(any(ClubCategory.class))).thenReturn(newCategory);
        when(clubCategoryMapper.toDTO(newCategory)).thenReturn(newCategoryDTO);

        // Act
        ClubCategoryDTO result = clubCategoryService.createClubCategory(request);

        // Assert
        assertNotNull(result);
        assertEquals("Văn hóa", result.getCategoryName());

        verify(clubCategoryRepository, times(1)).existsByCategoryNameIgnoreCase("  Văn hóa  ");
        verify(clubCategoryRepository, times(1)).save(any(ClubCategory.class));
        verify(clubCategoryMapper, times(1)).toDTO(newCategory);
    }

    @Test
    void createClubCategory_duplicateName_throwsAppException() {
        // Arrange
        CreateClubCategoryRequest request = new CreateClubCategoryRequest();
        request.setCategoryName(testCategoryName);

        when(clubCategoryRepository.existsByCategoryNameIgnoreCase(testCategoryName)).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubCategoryService.createClubCategory(request));
        assertEquals(ErrorCode.CLUB_CATEGORY_ALREADY_EXISTS, exception.getErrorCode());

        verify(clubCategoryRepository, times(1)).existsByCategoryNameIgnoreCase(testCategoryName);
        verify(clubCategoryRepository, never()).save(any());
    }

    // ================= updateClubCategory Tests =================

    @Test
    void updateClubCategory_validRequest_returnsUpdatedCategory() throws AppException {
        // Arrange
        UpdateClubCategoryRequest request = new UpdateClubCategoryRequest();
        request.setCategoryName("  Thể thao cập nhật  ");

        ClubCategory updatedCategory = ClubCategory.builder()
                .id(testCategoryId)
                .categoryName("Thể thao cập nhật")
                .build();

        ClubCategoryDTO updatedDTO = new ClubCategoryDTO();
        updatedDTO.setId(testCategoryId);
        updatedDTO.setCategoryName("Thể thao cập nhật");

        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(clubCategoryRepository.existsByCategoryNameIgnoreCaseAndIdNot("  Thể thao cập nhật  ", testCategoryId)).thenReturn(false);
        when(clubCategoryRepository.save(any(ClubCategory.class))).thenReturn(updatedCategory);
        when(clubCategoryMapper.toDTO(updatedCategory)).thenReturn(updatedDTO);

        // Act
        ClubCategoryDTO result = clubCategoryService.updateClubCategory(testCategoryId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Thể thao cập nhật", result.getCategoryName());

        verify(clubCategoryRepository, times(1)).findById(testCategoryId);
        verify(clubCategoryRepository, times(1)).save(any(ClubCategory.class));
        verify(clubCategoryMapper, times(1)).toDTO(updatedCategory);
    }

    @Test
    void updateClubCategory_nonExistingId_throwsNotFoundException() {
        // Arrange
        UpdateClubCategoryRequest request = new UpdateClubCategoryRequest();
        request.setCategoryName("New Name");

        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                clubCategoryService.updateClubCategory(testCategoryId, request));
        assertTrue(exception.getMessage().contains("Không tìm thấy thể loại câu lạc bộ"));

        verify(clubCategoryRepository, times(1)).findById(testCategoryId);
        verify(clubCategoryRepository, never()).save(any());
    }

    @Test
    void updateClubCategory_duplicateName_throwsAppException() {
        // Arrange
        UpdateClubCategoryRequest request = new UpdateClubCategoryRequest();
        request.setCategoryName("Existing Name");

        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(clubCategoryRepository.existsByCategoryNameIgnoreCaseAndIdNot("Existing Name", testCategoryId)).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubCategoryService.updateClubCategory(testCategoryId, request));
        assertEquals(ErrorCode.CLUB_CATEGORY_ALREADY_EXISTS, exception.getErrorCode());

        verify(clubCategoryRepository, times(1)).findById(testCategoryId);
        verify(clubCategoryRepository, never()).save(any());
    }

    // ================= deleteClubCategory Tests =================

    @Test
    void deleteClubCategory_asStaff_categoryNotInUse_deletesSuccessfully() {
        // Arrange
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(roleService.isStaff(testUserId)).thenReturn(true);
            when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
            doNothing().when(clubCategoryRepository).delete(testCategory);

            // Act
            assertDoesNotThrow(() -> clubCategoryService.deleteClubCategory(testCategoryId));

            // Assert
            verify(roleService, times(1)).isStaff(testUserId);
            verify(clubCategoryRepository, times(1)).findById(testCategoryId);
            verify(clubCategoryRepository, times(1)).delete(testCategory);
        }
    }

    @Test
    void deleteClubCategory_asStaff_categoryInUse_throwsAppException() {
        // Arrange
        Club club = new Club();
        club.setId(1L);
        club.setClubName("Test Club");
        Set<Club> clubs = new HashSet<>();
        clubs.add(club);
        testCategory.setClubs(clubs);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(roleService.isStaff(testUserId)).thenReturn(true);
            when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));

            // Act & Assert
            AppException exception = assertThrows(AppException.class, () ->
                    clubCategoryService.deleteClubCategory(testCategoryId));
            assertEquals(ErrorCode.CLUB_CATEGORY_IN_USE, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("đang có 1 câu lạc bộ sử dụng"));

            verify(roleService, times(1)).isStaff(testUserId);
            verify(clubCategoryRepository, times(1)).findById(testCategoryId);
            verify(clubCategoryRepository, never()).delete(any());
        }
    }

    @Test
    void deleteClubCategory_asStaff_nonExistingId_throwsNotFoundException() {
        // Arrange
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(roleService.isStaff(testUserId)).thenReturn(true);
            when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.empty());

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    clubCategoryService.deleteClubCategory(testCategoryId));
            assertTrue(exception.getMessage().contains("Không tìm thấy thể loại câu lạc bộ"));

            verify(roleService, times(1)).isStaff(testUserId);
            verify(clubCategoryRepository, times(1)).findById(testCategoryId);
            verify(clubCategoryRepository, never()).delete(any());
        }
    }

    @Test
    void deleteClubCategory_asNonStaff_throwsForbiddenException() {
        // Arrange
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(roleService.isStaff(testUserId)).thenReturn(false);

            // Act & Assert
            ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                    clubCategoryService.deleteClubCategory(testCategoryId));
            assertEquals("Chỉ STAFF mới có quyền xóa thể loại câu lạc bộ", exception.getMessage());

            verify(roleService, times(1)).isStaff(testUserId);
            verify(clubCategoryRepository, never()).findById(any());
        }
    }
}

