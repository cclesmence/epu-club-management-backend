package com.sep490.backendclubmanagement.service.admin.impl;

import com.sep490.backendclubmanagement.dto.request.StaffUpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.entity.News;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.admin.StaffNewsAdminService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffNewsAdminServiceImpl implements StaffNewsAdminService {

    private final NewsRepository newsRepository;
    private final RoleGuard roleGuard;

    private Long requireStaffId() {
        Long uid = roleGuard.getCurrentUserId();
        if (!roleGuard.isStaff(uid)) {
            throw new AccessDeniedException("Chỉ STAFF mới được thao tác.");
        }
        return uid;
    }

    private News getOrThrow(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin tức id=" + id));
    }

    /** Map thủ công để không phụ thuộc constructor của NewsData */
    private NewsData toDto(News n) {
        NewsData dto = new NewsData();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setThumbnailUrl(n.getThumbnailUrl());
        // entity là newsType
        dto.setNewsType(n.getNewsType());
        // tuỳ bạn muốn set thêm các trường khác
        return dto;
    }

    /** Bắt buộc text không rỗng */
    private static String requireText(String s, String fieldName) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        return s.trim();
    }

    @Override
    @Transactional
    public NewsData updateNews(Long id, StaffUpdateNewsRequest req) {
        Long staffId = requireStaffId();

        // YÊU CẦU: trường nào cũng phải nhập
        String title = requireText(req.getTitle(), "Tiêu đề");
        String content = requireText(req.getContent(), "Nội dung");
        String type = requireText(req.getType(), "Loại tin");
        String thumbnailUrl = requireText(req.getThumbnailUrl(), "Ảnh đại diện");

        News n = getOrThrow(id);
        if (n.isDeleted()) {
            throw new IllegalStateException("Bài đã bị xóa mềm, hãy khôi phục trước khi sửa.");
        }

        n.setTitle(title);
        n.setContent(content);
        n.setNewsType(type);          // entity dùng newsType
        n.setThumbnailUrl(thumbnailUrl);

        n.setUpdatedById(staffId);
        n.setUpdatedAt(LocalDateTime.now()); // BaseEntity.updatedAt là LocalDateTime

        newsRepository.saveAndFlush(n);
        return toDto(n);
    }

    @Override
    @Transactional
    public void hide(Long id) {
        requireStaffId();
        newsRepository.updateHidden(id, true);
    }

    @Override
    @Transactional
    public void unhide(Long id) {
        requireStaffId();
        newsRepository.updateHidden(id, false);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Long staffId = requireStaffId();
        newsRepository.softDelete(id, staffId, LocalDateTime.now()); // dùng LocalDateTime
    }

    @Override
    @Transactional
    public void restore(Long id) {
        requireStaffId();
        newsRepository.restore(id);
    }
}
