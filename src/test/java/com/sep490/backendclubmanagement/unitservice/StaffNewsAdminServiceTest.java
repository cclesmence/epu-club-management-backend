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
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class StaffNewsAdminServiceTest {

    @Mock
    private NewsRepository newsRepo;

    @Mock
    private RoleGuard guard;

    @InjectMocks
    private StaffNewsAdminServiceImpl service;

    private News news;
    private final Long STAFF_ID = 10L;

    @BeforeEach
    void setup() {
        news = new News();
        news.setId(1L);
        news.setTitle("Old");
        news.setContent("Old content");
        news.setThumbnailUrl("old.png");
        news.setNewsType("GENERAL");
        news.setDeleted(false);
    }

    // ========== requireStaffId tests ==========

    @Test
    void updateNews_nonStaff_throwsAccessDenied() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("A");
        req.setContent("B");
        req.setType("GENERAL");
        req.setThumbnailUrl("t.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.updateNews(1L, req));
    }

    // ========== updateNews validation tests ==========

    @Test
    void updateNews_emptyTitle_throwsIllegalArgument() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("");
        req.setContent("C");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNews(1L, req));
    }

    @Test
    void updateNews_emptyContent_throwsIllegalArgument() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("T");
        req.setContent(" ");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNews(1L, req));
    }

    @Test
    void updateNews_emptyType_throwsIllegalArgument() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("T");
        req.setContent("C");
        req.setType(" ");
        req.setThumbnailUrl("img.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNews(1L, req));
    }

    @Test
    void updateNews_emptyThumbnail_throwsIllegalArgument() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("T");
        req.setContent("C");
        req.setType("GENERAL");
        req.setThumbnailUrl("");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNews(1L, req));
    }

    // ========== updateNews: not found ==========

    @Test
    void updateNews_newsNotFound_throwsEntityNotFound() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("T");
        req.setContent("C");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);
        when(newsRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.updateNews(1L, req));
    }

    // ========== updateNews: soft deleted ==========

    @Test
    void updateNews_softDeleted_throwsIllegalState() {
        news.setDeleted(true);

        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("T");
        req.setContent("C");
        req.setType("GENERAL");
        req.setThumbnailUrl("img.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);
        when(newsRepo.findById(1L)).thenReturn(Optional.of(news));

        assertThrows(IllegalStateException.class,
                () -> service.updateNews(1L, req));
    }

    // ========== updateNews: success ==========

    @Test
    void updateNews_success() {
        StaffUpdateNewsRequest req = new StaffUpdateNewsRequest();
        req.setTitle("New Title");
        req.setContent("New Content");
        req.setType("NEWTYPE");
        req.setThumbnailUrl("new.png");

        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);
        when(newsRepo.findById(1L)).thenReturn(Optional.of(news));

        NewsData dto = service.updateNews(1L, req);

        assertEquals("New Title", dto.getTitle());
        assertEquals("New Content", dto.getContent());
        assertEquals("new.png", dto.getThumbnailUrl());
        verify(newsRepo).saveAndFlush(any(News.class));
    }

    // ========== hide() ==========

    @Test
    void hide_nonStaff_throwsAccessDenied() {
        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.hide(1L));
    }

    @Test
    void hide_success() {
        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        service.hide(1L);

        verify(newsRepo).updateHidden(1L, true);
    }

    // ========== unhide() ==========

    @Test
    void unhide_success() {
        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        service.unhide(1L);

        verify(newsRepo).updateHidden(1L, false);
    }

    // ========== softDelete() ==========

    @Test
    void softDelete_success() {
        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        service.softDelete(1L);

        verify(newsRepo).softDelete(eq(1L), eq(STAFF_ID), any(LocalDateTime.class));
    }

    // ========== restore() ==========

    @Test
    void restore_success() {
        when(guard.getCurrentUserId()).thenReturn(STAFF_ID);
        when(guard.isStaff(STAFF_ID)).thenReturn(true);

        service.restore(1L);

        verify(newsRepo).restore(1L);
    }
}
