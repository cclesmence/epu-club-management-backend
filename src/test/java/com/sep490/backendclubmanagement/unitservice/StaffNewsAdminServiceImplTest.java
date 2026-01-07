package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.StaffUpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.entity.News;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.admin.impl.StaffNewsAdminServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class StaffNewsAdminServiceImplTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private RoleGuard roleGuard;

    @InjectMocks
    private StaffNewsAdminServiceImpl staffNewsAdminService;

    private News existingNews;
    private Long staffId;

    @BeforeEach
    void setup() {
        staffId = 100L;

        existingNews = new News();
        existingNews.setId(1L);
        existingNews.setTitle("Old title");
        existingNews.setContent("Old content");
        existingNews.setNewsType("OLD_TYPE");
        existingNews.setThumbnailUrl("old.png");
        existingNews.setDeleted(false);
    }

    // ========== updateNews ==========

    @Test
    void updateNews_happyPath_updatesFieldsAndReturnsDto() {
        // Arrange
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("  New title  ");
        req.setContent("  New content  ");
        req.setType("GENERAL");
        req.setThumbnailUrl("new.png");

        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);
        when(newsRepository.findById(existingNews.getId()))
                .thenReturn(Optional.of(existingNews));
        when(newsRepository.saveAndFlush(any(News.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        NewsData dto = staffNewsAdminService.updateNews(existingNews.getId(), req);

        // Assert
        assertNotNull(dto);
        assertEquals("New title", existingNews.getTitle());
        assertEquals("New content", existingNews.getContent());
        assertEquals("GENERAL", existingNews.getNewsType());
        assertEquals("new.png", existingNews.getThumbnailUrl());
        assertEquals(staffId, existingNews.getUpdatedById());
        assertNotNull(existingNews.getUpdatedAt());

        verify(roleGuard, times(1)).getCurrentUserId();
        verify(roleGuard, times(1)).isStaff(staffId);
        verify(newsRepository, times(1)).saveAndFlush(existingNews);
    }

    @Test
    void updateNews_notStaff_throwsAccessDeniedException() {
        // Arrange
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("Title");
        req.setContent("Content");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(false);

        // Act & Assert
        assertThrows(
                AccessDeniedException.class,
                () -> staffNewsAdminService.updateNews(1L, req)
        );

        verify(newsRepository, never()).findById(anyLong());
    }

    @Test
    void updateNews_newsNotFound_throwsEntityNotFound() {
        // Arrange
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("Title");
        req.setContent("Content");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);
        when(newsRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                EntityNotFoundException.class,
                () -> staffNewsAdminService.updateNews(999L, req)
        );
    }

    @Test
    void updateNews_deletedNews_throwsIllegalStateException() {
        // Arrange
        existingNews.setDeleted(true);

        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("New");
        req.setContent("New");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);
        when(newsRepository.findById(existingNews.getId()))
                .thenReturn(Optional.of(existingNews));

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> staffNewsAdminService.updateNews(existingNews.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Bài đã bị xóa mềm"));

        verify(newsRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateNews_emptyTitle_throwsIllegalArgumentException() {
        // Arrange
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("   "); // empty
        req.setContent("Content");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> staffNewsAdminService.updateNews(1L, req)
        );
        assertTrue(ex.getMessage().contains("Tiêu đề không được để trống"));
    }

    // ========== hide / unhide ==========

    @Test
    void hide_validStaff_callsRepository() {
        // Arrange
        Long newsId = 5L;
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);

        // Act
        staffNewsAdminService.hide(newsId);

        // Assert
        verify(newsRepository, times(1)).updateHidden(newsId, true);
    }

    @Test
    void unhide_validStaff_callsRepository() {
        // Arrange
        Long newsId = 6L;
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);

        // Act
        staffNewsAdminService.unhide(newsId);

        // Assert
        verify(newsRepository, times(1)).updateHidden(newsId, false);
    }

    @Test
    void hide_notStaff_throwsAccessDenied() {
        // Arrange
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(false);

        // Act & Assert
        assertThrows(
                AccessDeniedException.class,
                () -> staffNewsAdminService.hide(1L)
        );
        verify(newsRepository, never()).updateHidden(anyLong(), anyBoolean());
    }

    // ========== softDelete / restore ==========

    @Test
    void softDelete_validStaff_callsRepository() {
        // Arrange
        Long newsId = 7L;
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);

        // Act
        staffNewsAdminService.softDelete(newsId);

        // Assert
        verify(newsRepository, times(1))
                .softDelete(eq(newsId), eq(staffId), any(LocalDateTime.class));
    }

    @Test
    void restore_validStaff_callsRepository() {
        // Arrange
        Long newsId = 8L;
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(true);

        // Act
        staffNewsAdminService.restore(newsId);

        // Assert
        verify(newsRepository, times(1)).restore(newsId);
    }

    @Test
    void softDelete_notStaff_throwsAccessDenied() {
        // Arrange
        when(roleGuard.getCurrentUserId()).thenReturn(staffId);
        when(roleGuard.isStaff(staffId)).thenReturn(false);

        // Act & Assert
        assertThrows(
                AccessDeniedException.class,
                () -> staffNewsAdminService.softDelete(1L)
        );
        verify(newsRepository, never()).softDelete(anyLong(), anyLong(), any());
    }
}
