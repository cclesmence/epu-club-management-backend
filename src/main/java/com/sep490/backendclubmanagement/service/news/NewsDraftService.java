package com.sep490.backendclubmanagement.service.news;

import com.sep490.backendclubmanagement.dto.request.CreateDraftRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateDraftRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.mapper.NewsMapper;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import com.sep490.backendclubmanagement.repository.RequestNewsRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsDraftService {

    private final NewsRepository newsRepo;
    private final RequestNewsRepository requestRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final RoleGuard guard;
    private final NewsMapper newsMapper;
    private final WebSocketService webSocketService;       // realtime cũ
    private final NotificationService notificationService; // notification DB

    // ========== CREATE DRAFT ==========
    @Transactional
    public NewsData createDraft(Long me, CreateDraftRequest body) {
        User creator = userRepo.findById(me).orElseThrow();

        Club club = null;
        if (guard.isStaff(me)) {
            if (body.getClubId() != null) {
                club = clubRepo.findById(body.getClubId()).orElseThrow();
            }
        } else {
            if (body.getClubId() == null) {
                throw new IllegalStateException("Thiếu ngữ cảnh CLB cho người dùng không phải STAFF.");
            }
            final Long clubId = body.getClubId();

            if (body.getTeamId() != null) {
                boolean leadOfTeam = guard.isTeamLead(me, clubId, body.getTeamId());
                if (!leadOfTeam) {
                    throw new SecurityException("Bạn không có quyền tạo nháp cho team này.");
                }
            } else {
                boolean manager = guard.isClubManager(me, clubId);
                if (!manager) {
                    throw new SecurityException("Bạn không có quyền tạo nháp cho CLB này.");
                }
            }
            club = clubRepo.findById(clubId).orElseThrow();
        }

        News draft = News.builder()
                .title(body.getTitle().trim())
                .content(body.getContent().trim())
                .thumbnailUrl(body.getThumbnailUrl())
                .newsType(body.getNewsType())
                .isDraft(true)
                .createdBy(creator)
                .club(club)
                .build();

        newsRepo.save(draft);
        return newsMapper.toDto(draft);
    }

    // ========== UPDATE DRAFT ==========
    @Transactional
    public NewsData updateDraft(Long me, Long newsId, UpdateDraftRequest body) {
        News draft = newsRepo.findById(newsId).orElseThrow();

        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Bản ghi không phải nháp.");
        }

        Long clubId = draft.getClub() != null ? draft.getClub().getId() : null;

        boolean canEdit =
                guard.isStaff(me) ||
                        draft.getCreatedBy().getId().equals(me) ||
                        (clubId != null && (guard.canApproveAtClub(me, clubId) || guard.isLead(me, clubId)));

        if (!canEdit) {
            throw new SecurityException("Bạn không có quyền sửa nháp này.");
        }

        if (body.getTitle() != null)         draft.setTitle(body.getTitle());
        if (body.getContent() != null)       draft.setContent(body.getContent());
        if (body.getThumbnailUrl() != null)  draft.setThumbnailUrl(body.getThumbnailUrl());
        if (body.getNewsType() != null)      draft.setNewsType(body.getNewsType());
        if (body.getIsSpotlight() != null)   draft.setIsSpotlight(body.getIsSpotlight());

        newsRepo.save(draft);
        return newsMapper.toDto(draft);
    }

    // ========== DELETE DRAFT ==========
    @Transactional
    public void deleteDraft(Long me, Long newsId) {
        News draft = newsRepo.findById(newsId).orElseThrow();

        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Bản ghi không phải nháp.");
        }

        Long clubId = draft.getClub() != null ? draft.getClub().getId() : null;

        boolean canDelete =
                guard.isStaff(me) ||
                        draft.getCreatedBy().getId().equals(me) ||
                        (clubId != null && (guard.canApproveAtClub(me, clubId) || guard.isLead(me, clubId)));

        if (!canDelete) {
            throw new SecurityException("Bạn không có quyền xóa nháp này.");
        }

        if (requestRepo.existsPendingByNewsId(newsId)) {
            throw new IllegalStateException("Nháp đang gắn vào một yêu cầu chưa xử lý. Hãy hủy hoặc hoàn tất yêu cầu trước khi xóa.");
        }

        newsRepo.delete(draft);
    }

    // ========== LIST DRAFTS ==========
    @Transactional(readOnly = true)
    public Page<NewsData> listDrafts(Long me, Long clubId, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);

        // sort giống logic cũ: updatedAt DESC, id DESC
        var sort = Sort.by(Sort.Direction.DESC, "updatedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));

        var pageable = PageRequest.of(p, s, sort);

        // để DB lọc + phân trang
        Page<News> draftsPage = newsRepo.findDraftsVisibleToUser(me, clubId, pageable);

        // map sang DTO, NHƯNG KHÔNG N+1 vì đã EntityGraph(createdBy, club)
        return draftsPage.map(newsMapper::toDto);
    }

    // ========== SUBMIT DRAFT -> REQUEST ==========
    @Transactional
    public Map<String, Object> submitDraftToRequest(Long me, Long newsId) throws AppException {
        News draft = newsRepo.findById(newsId).orElseThrow();
        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Bản ghi không phải nháp.");
        }

        Long clubId = draft.getClub() != null ? draft.getClub().getId() : null;

        boolean canSubmit =
                draft.getCreatedBy().getId().equals(me) ||
                        guard.isStaff(me) ||
                        (clubId != null && (guard.canApproveAtClub(me, clubId) || guard.isLead(me, clubId)));

        if (!canSubmit) throw new SecurityException("Bạn không có quyền submit nháp này.");

        if (requestRepo.existsPendingByNewsId(newsId)) {
            throw new IllegalStateException("Nháp này đã có yêu cầu đang chờ xử lý.");
        }

        User actor = userRepo.findById(me).orElseThrow();

        RequestStatus startStatus;
        Team team = null;

        if (guard.isStaff(me)) {
            startStatus = RequestStatus.PENDING_UNIVERSITY;
        } else if (clubId != null && guard.isClubManager(me, clubId)) {
            // Chủ nhiệm / Phó → không có team
            startStatus = RequestStatus.PENDING_UNIVERSITY;
            team = null;  // ép null ngay tại đây

        } else if (clubId != null && guard.isLead(me, clubId)) {
            // Trưởng ban → phải có team
            startStatus = RequestStatus.PENDING_CLUB;
            team = guard.findLeadTeamInClub(me, clubId)
                    .orElseThrow(() -> new SecurityException("Không tìm thấy team trưởng ban."));
        } else {
            throw new SecurityException("Bạn không có quyền submit nháp này.");
        }

        RequestNews req = RequestNews.builder()
                .requestTitle(draft.getTitle())
                .description(draft.getContent())
                .responseMessage("")
                .status(startStatus)
                .createdBy(actor)
                .club(draft.getClub())
                .team(team)
                .thumbnailUrl(draft.getThumbnailUrl())
                .newsType(draft.getNewsType())
                .news(null)
                .build();

        requestRepo.save(req);
        newsRepo.delete(draft);

        // Realtime broadcast cũ
        Map<String, Object> payload = Map.of(
                "requestId", req.getId(),
                "clubId", clubId,
                "status", req.getStatus().name()
        );

        if (startStatus == RequestStatus.PENDING_CLUB) {
            webSocketService.broadcastToClub(clubId, "NEWS_REQUEST", "CREATED", payload);
        } else {
            webSocketService.broadcastToSystemRole("STAFF", "NEWS_REQUEST", "CREATED", payload);
        }

        // Notification DB: trưởng ban submit -> gửi Chủ nhiệm/Phó (PENDING_CLUB)
        if (startStatus == RequestStatus.PENDING_CLUB && clubId != null) {
            String actionUrl = "/myclub/" + clubId + "/news/requests/" + req.getId();
            String title = "Yêu cầu tin tức mới từ ban trong CLB";
            String message = actor.getFullName() + " đã gửi yêu cầu tin tức cần duyệt trong CLB.";

            var managerIds = notificationService.getClubManagers(clubId);
            notificationService.sendToUsers(
                    managerIds,
                    me,
                    title,
                    message,
                    NotificationType.NEWS_PENDING_APPROVAL,
                    NotificationPriority.NORMAL,
                    actionUrl,
                    clubId,
                    null,
                    team != null ? team.getId() : null,
                    req.getId()
            );
        }

        // Notification DB: Chủ nhiệm/Phó submit nháp -> PENDING_UNIVERSITY -> gửi Staff
        if (startStatus == RequestStatus.PENDING_UNIVERSITY && clubId != null && !guard.isStaff(me)) {
            String staffTitle = "Yêu cầu tin tức mới từ CLB " + draft.getClub().getClubName();
            String staffMessage = "CLB " + draft.getClub().getClubName()
                    + " đã gửi yêu cầu tin tức \"" + req.getRequestTitle() + "\" cần duyệt.";

            String staffActionUrl = "/staff/news/requests/" + req.getId();

            List<User> staffUsers = userRepo.findBySystemRole_RoleNameIgnoreCase("STAFF");
            List<Long> staffIds = staffUsers.stream().map(User::getId).toList();

            notificationService.sendToUsers(
                    staffIds,
                    me,
                    staffTitle,
                    staffMessage,
                    NotificationType.NEWS_PENDING_APPROVAL,
                    NotificationPriority.NORMAL,
                    staffActionUrl,
                    clubId,
                    null,
                    team != null ? team.getId() : null,
                    req.getId()
            );
        }

        return payload;
    }

    // ========== STAFF PUBLISH DRAFT ==========
    @Transactional
    public NewsData publishDraftByStaff(Long me, Long newsId) throws AppException {
        if (!guard.isStaff(me)) {
            throw new SecurityException("Chỉ Staff được publish trực tiếp.");
        }

        News draft = newsRepo.findById(newsId).orElseThrow();
        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Bản ghi không phải nháp.");
        }

        draft.setIsDraft(false);
        newsRepo.save(draft);

        // Realtime broadcast cũ
        webSocketService.broadcastSystemWide("NEWS", "PUBLISHED", newsMapper.toDto(draft));

        // Notification DB: gửi cho người tạo + (nếu có) chủ nhiệm CLB
        String actionUrl = "/news/" + draft.getId();
        String title = "Tin tức đã được đăng";
        String message = "Bài viết \"" + draft.getTitle() + "\" đã được staff đăng.";

        Long creatorId = draft.getCreatedBy() != null ? draft.getCreatedBy().getId() : null;
        Long clubId = draft.getClub() != null ? draft.getClub().getId() : null;

        if (creatorId != null) {
            try {
                notificationService.sendToUser(
                        creatorId,
                        me,
                        title,
                        message,
                        NotificationType.NEWS_PUBLISHED,
                        NotificationPriority.HIGH,
                        actionUrl,
                        clubId,
                        draft.getId(),
                        null,
                        null,
                        null
                );
            } catch (AppException e) {
                // Không gửi được notif cho creator thì bỏ qua
            }
        }

        if (clubId != null) {
            var managerIds = notificationService.getClubManagers(clubId);
            notificationService.sendToUsers(
                    managerIds,
                    me,
                    title,
                    message,
                    NotificationType.NEWS_PUBLISHED,
                    NotificationPriority.NORMAL,
                    actionUrl,
                    clubId,
                    draft.getId(),
                    null,
                    null
            );
        }

        return newsMapper.toDto(draft);
    }

    // ========== GET DRAFT DETAIL ==========
    @Transactional(readOnly = true)
    public NewsData getDraftDetail(Long me, Long newsId) {
        News draft = newsRepo.findById(newsId).orElseThrow();

        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Bản ghi không phải nháp.");
        }

        Long clubId = draft.getClub() != null ? draft.getClub().getId() : null;

        boolean canView =
                guard.isStaff(me) ||
                        draft.getCreatedBy().getId().equals(me) ||
                        (clubId != null && (guard.canApproveAtClub(me, clubId) || guard.isLead(me, clubId)));

        if (!canView) throw new SecurityException("Bạn không có quyền xem nháp này.");

        return newsMapper.toDto(draft);
    }
}
