package com.sep490.backendclubmanagement.service.news;

import com.sep490.backendclubmanagement.dto.request.ApproveNewsRequest;
import com.sep490.backendclubmanagement.dto.request.CreateNewsRequest;
import com.sep490.backendclubmanagement.dto.request.RejectNewsRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsRequestResponse;
import com.sep490.backendclubmanagement.dto.response.PublishResult;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.mapper.NewsMapper;
import com.sep490.backendclubmanagement.mapper.RequestNewsMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsWorkflowService {

    private final RequestNewsRepository requestRepo;
    private final NewsRepository newsRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final RoleGuard guard;
    private final RequestNewsMapper mapper;
    private final NewsMapper newsMapper;
    private final TeamRepository teamRepo;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;

    // ========== CREATE REQUEST ==========
    @Transactional
    public NewsRequestResponse createRequest(Long me, CreateNewsRequest dto) throws AppException {
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        String desc  = dto.getContent() == null ? "" : dto.getContent().trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Tiêu đề không được để trống.");
        if (desc.isEmpty())  throw new IllegalArgumentException("Nội dung không được để trống.");
        if (dto.getClubId() == null) throw new IllegalArgumentException("Thiếu clubId.");

        User creator = userRepo.findById(me).orElseThrow();
        Club club    = clubRepo.findById(dto.getClubId()).orElseThrow();

        boolean isStaff      = guard.isStaff(me);
        boolean isManager    = guard.canApproveAtClub(me, club.getId()); // chủ nhiệm / phó
        boolean isLeadInClub = guard.isLead(me, club.getId());           // trưởng ban

        RequestStatus startStatus;
        News attachedNews = null;
        Team team = null;

        if (isStaff) {
            startStatus = RequestStatus.PENDING_UNIVERSITY;

        } else if (isManager) {
            // CHỦ NHIỆM / PHÓ -> không gán team
            dto.setTeamId(null);
            startStatus = RequestStatus.PENDING_UNIVERSITY;

            attachedNews = News.builder()
                    .title(title)
                    .content(desc)
                    .thumbnailUrl(dto.getThumbnailUrl()) // có thể null
                    .newsType(dto.getNewsType())
                    .isDraft(false)
                    .createdBy(creator)
                    .club(club)
                    .build();
            newsRepo.save(attachedNews);

        } else if (isLeadInClub) {
            // TRƯỞNG BAN -> bắt buộc team
            if (dto.getTeamId() == null)
                throw new IllegalArgumentException("Thiếu teamId cho trưởng ban.");
            if (!guard.isTeamLead(me, club.getId(), dto.getTeamId()))
                throw new SecurityException("Bạn không có quyền tạo request cho team này.");

            startStatus = RequestStatus.PENDING_CLUB;
            team = teamRepo.findById(dto.getTeamId()).orElseThrow();

        } else {
            throw new SecurityException("Bạn không có quyền tạo request trong CLB này.");
        }

        // === TẠO REQUEST ===
        RequestNews req = RequestNews.builder()
                .requestTitle(title)
                .description(desc)
                .responseMessage("")
                .status(startStatus)
                .createdBy(creator)
                .club(club)
                .team(team) // null nếu manager
                .thumbnailUrl(dto.getThumbnailUrl())
                .newsType(dto.getNewsType())
                .news(attachedNews)
                .build();

        requestRepo.save(req);

        // ==== NOTIFICATION & REALTIME ====
        Map<String, Object> payload = Map.of(
                "requestId", req.getId(),
                "clubId", club.getId(),
                "status", req.getStatus().name()
        );

        if (startStatus == RequestStatus.PENDING_CLUB) {
            webSocketService.broadcastToClub(club.getId(), "NEWS_REQUEST", "CREATED", payload);
        } else {
            webSocketService.broadcastToSystemRole("STAFF", "NEWS_REQUEST", "CREATED", payload);
        }

        // Notification DB:
        String actionUrl = "/myclub/" + club.getId() + "/news/requests/" + req.getId();

        // Nếu trưởng ban tạo request -> gửi Chủ nhiệm/Phó chủ nhiệm
        if (startStatus == RequestStatus.PENDING_CLUB) {
            String notiTitle = "Yêu cầu tin tức mới từ ban trong CLB";
            String msg = creator.getFullName() + " đã gửi yêu cầu tin tức cần duyệt.";

            var managerIds = notificationService.getClubManagers(club.getId());
            notificationService.sendToUsers(
                    managerIds,
                    me,
                    notiTitle,
                    msg,
                    NotificationType.NEWS_PENDING_APPROVAL,
                    NotificationPriority.NORMAL,
                    actionUrl,
                    club.getId(),
                    attachedNews != null ? attachedNews.getId() : null,
                    team != null ? team.getId() : null,
                    req.getId()
            );
        }

        // Nếu Chủ nhiệm/Phó tạo request trực tiếp (PENDING_UNIVERSITY, không phải staff) -> gửi Staff
        if (startStatus == RequestStatus.PENDING_UNIVERSITY && !isStaff) {
            String staffTitle = "Yêu cầu tin tức mới từ CLB " + club.getClubName();
            String staffMsg = "CLB " + club.getClubName()
                    + " đã tạo yêu cầu tin tức \"" + req.getRequestTitle() + "\" cần duyệt.";

            String staffActionUrl = "/staff/news/requests/" + req.getId();

            List<User> staffUsers = userRepo.findBySystemRole_RoleNameIgnoreCase("STAFF");
            List<Long> staffIds = staffUsers.stream().map(User::getId).toList();

            notificationService.sendToUsers(
                    staffIds,
                    me,
                    staffTitle,
                    staffMsg,
                    NotificationType.NEWS_PENDING_APPROVAL,
                    NotificationPriority.NORMAL,
                    staffActionUrl,
                    club.getId(),
                    attachedNews != null ? attachedNews.getId() : null,
                    team != null ? team.getId() : null,
                    req.getId()
            );
        }

        RequestNews detail = requestRepo.findDetailById(req.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== UPDATE REQUEST WHEN PENDING ==========
    @Transactional
    public NewsRequestResponse updatePendingRequest(Long me, Long requestId, UpdateNewsRequest body) {
        RequestNews r = requestRepo.findDetailById(requestId).orElseThrow();
        RequestStatus st = r.getStatus();
        Long clubId = r.getClub() != null ? r.getClub().getId() : null;

        boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(me);
        boolean isStaff   = guard.isStaff(me);
        boolean isClubMgr = clubId != null && guard.canApproveAtClub(me, clubId);

        boolean canEdit;
        if (st == RequestStatus.PENDING_CLUB) {
            canEdit = isCreator || isClubMgr || isStaff;
        } else if (st == RequestStatus.PENDING_UNIVERSITY) {
            canEdit = isClubMgr || isStaff;
        } else {
            throw new IllegalStateException("Chỉ được sửa khi request đang ở trạng thái PENDING.");
        }

        if (!canEdit) throw new SecurityException("Bạn không có quyền sửa request này ở trạng thái hiện tại.");

        if (body.getTitle() != null && !body.getTitle().isBlank()) r.setRequestTitle(body.getTitle().trim());
        if (body.getContent() != null && !body.getContent().isBlank()) r.setDescription(body.getContent().trim());
        if (body.getThumbnailUrl() != null) r.setThumbnailUrl(body.getThumbnailUrl());
        if (body.getNewsType() != null) r.setNewsType(body.getNewsType());

        News attached = r.getNews();
        if (attached != null && Boolean.TRUE.equals(attached.getIsDraft())) {
            if (body.getTitle() != null && !body.getTitle().isBlank()) attached.setTitle(body.getTitle().trim());
            if (body.getContent() != null && !body.getContent().isBlank()) attached.setContent(body.getContent().trim());
            if (body.getThumbnailUrl() != null) attached.setThumbnailUrl(body.getThumbnailUrl());
            if (body.getNewsType() != null) attached.setNewsType(body.getNewsType());
            newsRepo.save(attached);
        }

        requestRepo.save(r);
        RequestNews detail = requestRepo.findDetailById(r.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== CLUB APPROVE & SUBMIT ==========
    @Transactional
    public NewsRequestResponse clubApproveAndSubmit(Long clubLeaderId, Long requestId, ApproveNewsRequest body) throws AppException {
        RequestNews r = requestRepo.findById(requestId).orElseThrow();

        if (!guard.canApproveAtClub(clubLeaderId, r.getClub().getId()))
            throw new SecurityException("Chỉ Chủ nhiệm/Phó được duyệt.");
        if (r.getStatus() != RequestStatus.PENDING_CLUB)
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING_CLUB.");

        if (body != null && body.getContent() != null) r.setDescription(body.getContent());

        r.setStatus(RequestStatus.PENDING_UNIVERSITY);
        r.setResponseMessage("Chủ nhiệm CLB đã duyệt và gửi lên cấp trường.");
        requestRepo.save(r);

        // Realtime: gửi cho staff (và người tạo thấy update)
        webSocketService.broadcastToSystemRole(
                "STAFF", "NEWS_REQUEST", "FORWARDED",
                Map.of("requestId", r.getId(), "clubId", r.getClub().getId(), "status", r.getStatus().name())
        );
        webSocketService.broadcastToUser(
                r.getCreatedBy().getId(), "NEWS_REQUEST", "UPDATED",
                Map.of("requestId", r.getId(), "status", r.getStatus().name())
        );

        // Notification cho người tạo (trưởng ban)
        String actionUrlForCreator = buildCreatorRequestUrl(r);

        String titleForCreator = "Yêu cầu tin tức đã được Chủ nhiệm duyệt";
        String msgForCreator = "Yêu cầu \"" + r.getRequestTitle() + "\" đã được Chủ nhiệm duyệt và gửi lên nhà trường.";

        notificationService.sendToUser(
                r.getCreatedBy().getId(),
                clubLeaderId,
                titleForCreator,
                msgForCreator,
                NotificationType.NEWS_APPROVED,
                NotificationPriority.NORMAL,
                actionUrlForCreator,
                r.getClub() != null ? r.getClub().getId() : null,
                r.getNews() != null ? r.getNews().getId() : null,
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getId(),
                null
        );

        // Notification cho Staff
        String staffTitle = "Yêu cầu tin tức mới từ CLB " + r.getClub().getClubName();
        String staffMsg = "CLB " + r.getClub().getClubName()
                + " đã gửi yêu cầu tin tức \"" + r.getRequestTitle() + "\" lên cấp trường.";
        String staffActionUrl = "/staff/news/requests/" + r.getId();

        List<User> staffUsers = userRepo.findBySystemRole_RoleNameIgnoreCase("STAFF");
        List<Long> staffIds = staffUsers.stream().map(User::getId).toList();

        notificationService.sendToUsers(
                staffIds,
                clubLeaderId,
                staffTitle,
                staffMsg,
                NotificationType.NEWS_PENDING_APPROVAL,
                NotificationPriority.NORMAL,
                staffActionUrl,
                r.getClub() != null ? r.getClub().getId() : null,
                r.getNews() != null ? r.getNews().getId() : null,
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getId()
        );

        RequestNews detail = requestRepo.findDetailById(r.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== CLUB PRESIDENT REJECT ==========
    @Transactional
    public NewsRequestResponse clubPresidentReject(Long userId, Long requestId, RejectNewsRequest body) throws AppException {
        RequestNews r = requestRepo.findById(requestId).orElseThrow();
        Long clubId = r.getClub().getId();

        if (!guard.canRejectAtClub(userId, clubId))
            throw new SecurityException("Chỉ Chủ nhiệm được từ chối ở cấp CLB.");
        if (r.getStatus() != RequestStatus.PENDING_CLUB)
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING_CLUB.");

        r.setStatus(RequestStatus.REJECTED_CLUB);
        r.setResponseMessage(body == null ? "" : body.getReason());
        requestRepo.save(r);

        // Realtime: gửi cho người tạo & toàn CLB
        Map<String, Object> payload = Map.of("requestId", r.getId(), "status", r.getStatus().name());
        webSocketService.broadcastToUser(r.getCreatedBy().getId(), "NEWS_REQUEST", "REJECTED", payload);
        webSocketService.broadcastToClub(clubId, "NEWS_REQUEST", "REJECTED", payload);

        // Notification DB: gửi cho người tạo
        String actionUrlForCreator = buildCreatorRequestUrl(r);
        String title = "Yêu cầu tin tức bị từ chối ở cấp CLB";
        String message = "Yêu cầu \"" + r.getRequestTitle() + "\" đã bị Chủ nhiệm CLB từ chối.";

        notificationService.sendToUser(
                r.getCreatedBy().getId(),
                userId,
                title,
                message,
                NotificationType.NEWS_REJECTED,
                NotificationPriority.NORMAL,
                actionUrlForCreator,
                clubId,
                r.getNews() != null ? r.getNews().getId() : null,
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getId(),
                null
        );

        RequestNews detail = requestRepo.findDetailById(r.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== STAFF APPROVE & PUBLISH ==========
    @Transactional
    public NewsRequestResponse staffApproveAndPublish(Long staffId, Long requestId, ApproveNewsRequest body) throws AppException {
        RequestNews r = requestRepo.findById(requestId).orElseThrow();

        if (!guard.isStaff(staffId)) throw new SecurityException("Chỉ Staff được duyệt ở cấp trường.");
        if (r.getStatus() != RequestStatus.PENDING_UNIVERSITY)
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING_UNIVERSITY.");

        News usedNews;
        if (r.getNews() != null && Boolean.TRUE.equals(r.getNews().getIsDraft())) {
            usedNews = r.getNews();
            usedNews.setIsDraft(false);
            if (body != null) {
                if (body.getTitle() != null) usedNews.setTitle(body.getTitle());
                if (body.getContent() != null) usedNews.setContent(body.getContent());
                if (body.getThumbnailUrl() != null) usedNews.setThumbnailUrl(body.getThumbnailUrl());
                if (body.getNewsType() != null) usedNews.setNewsType(body.getNewsType());
                if (body.getIsSpotlight() != null) usedNews.setIsSpotlight(body.getIsSpotlight());
            }
            newsRepo.save(usedNews);
        } else if (r.getNews() != null) {
            usedNews = r.getNews();
        } else {
            String title = (body != null && body.getTitle() != null) ? body.getTitle() : r.getRequestTitle();
            String content = (body != null && body.getContent() != null) ? body.getContent() : r.getDescription();
            String thumb = (body != null && body.getThumbnailUrl() != null) ? body.getThumbnailUrl() : r.getThumbnailUrl();
            String type = (body != null && body.getNewsType() != null) ? body.getNewsType() : r.getNewsType();

            usedNews = News.builder()
                    .title(title)
                    .content(content)
                    .thumbnailUrl(thumb)
                    .newsType(type)
                    .isSpotlight(body != null && Boolean.TRUE.equals(body.getIsSpotlight()))
                    .isDraft(false)
                    .createdBy(r.getCreatedBy())
                    .club(r.getClub())
                    .build();
            newsRepo.save(usedNews);
            r.setNews(usedNews);
        }

        if (r.getThumbnailUrl() == null && usedNews.getThumbnailUrl() != null) r.setThumbnailUrl(usedNews.getThumbnailUrl());
        if (r.getNewsType() == null && usedNews.getNewsType() != null) r.setNewsType(usedNews.getNewsType());

        r.setStatus(RequestStatus.APPROVED_UNIVERSITY);
        r.setResponseMessage("Staff approved and published.");
        requestRepo.saveAndFlush(r);

        // Realtime: broadcast toàn hệ thống + cho người tạo
        webSocketService.broadcastSystemWide("NEWS", "PUBLISHED", newsMapper.toDto(usedNews));
        webSocketService.broadcastToUser(r.getCreatedBy().getId(), "NEWS_REQUEST", "APPROVED",
                Map.of("requestId", r.getId(), "status", r.getStatus().name()));

        // Notification DB:
        String newsUrl = "/news/" + usedNews.getId();
        String titleForCreator = "Yêu cầu tin tức đã được duyệt và đăng";
        String msgForCreator = "Bài viết \"" + usedNews.getTitle() + "\" đã được nhà trường duyệt và đăng.";

        notificationService.sendToUser(
                r.getCreatedBy().getId(),
                staffId,
                titleForCreator,
                msgForCreator,
                NotificationType.NEWS_APPROVED,
                NotificationPriority.HIGH,
                newsUrl,
                r.getClub() != null ? r.getClub().getId() : null,
                usedNews.getId(),
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getId(),
                null
        );

        // Thông báo cho Chủ nhiệm/Phó chủ nhiệm (nếu là tin CLB)
        if (r.getClub() != null) {
            var managerIds = notificationService.getClubManagers(r.getClub().getId());
            String titleForManagers = "Tin tức của CLB đã được đăng";
            String msgForManagers = "Bài viết \"" + usedNews.getTitle() + "\" của CLB đã được nhà trường duyệt và đăng.";

            notificationService.sendToUsers(
                    managerIds,
                    staffId,
                    titleForManagers,
                    msgForManagers,
                    NotificationType.NEWS_PUBLISHED,
                    NotificationPriority.NORMAL,
                    newsUrl,
                    r.getClub().getId(),
                    usedNews.getId(),
                    r.getTeam() != null ? r.getTeam().getId() : null,
                    r.getId()
            );
        }

        RequestNews detail = requestRepo.findDetailById(r.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== STAFF REJECT ==========
    @Transactional
    public NewsRequestResponse staffReject(Long staffId, Long requestId, RejectNewsRequest body) throws AppException {
        RequestNews r = requestRepo.findById(requestId).orElseThrow();

        if (!guard.isStaff(staffId)) throw new SecurityException("Chỉ Staff được từ chối ở cấp trường.");
        if (r.getStatus() != RequestStatus.PENDING_UNIVERSITY)
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING_UNIVERSITY.");

        r.setStatus(RequestStatus.REJECTED_UNIVERSITY);
        r.setResponseMessage(body == null ? "" : body.getReason());
        requestRepo.save(r);

        // Realtime: gửi cho người tạo + staff dashboard
        Map<String, Object> payload = Map.of("requestId", r.getId(), "status", r.getStatus().name());
        webSocketService.broadcastToUser(r.getCreatedBy().getId(), "NEWS_REQUEST", "REJECTED", payload);
        webSocketService.broadcastToSystemRole("STAFF", "NEWS_REQUEST", "REJECTED", payload);

        // Notification DB:
        String actionUrlForCreator = buildCreatorRequestUrl(r);
        String title = "Yêu cầu tin tức bị từ chối ở cấp trường";
        String message = "Yêu cầu \"" + r.getRequestTitle() + "\" đã bị nhà trường từ chối.";

        notificationService.sendToUser(
                r.getCreatedBy().getId(),
                staffId,
                title,
                message,
                NotificationType.NEWS_REJECTED,
                NotificationPriority.NORMAL,
                actionUrlForCreator,
                r.getClub() != null ? r.getClub().getId() : null,
                r.getNews() != null ? r.getNews().getId() : null,
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getId(),
                null
        );

        RequestNews detail = requestRepo.findDetailById(r.getId()).orElseThrow();
        return mapper.toDto(detail);
    }

    // ========== STAFF DIRECT PUBLISH ==========
    @Transactional
    public PublishResult staffDirectPublish(Long me, ApproveNewsRequest body) {
        if (!guard.isStaff(me)) throw new SecurityException("Chỉ Staff được publish trực tiếp.");

        String title = body.getTitle() == null ? "" : body.getTitle().trim();
        String content = body.getContent() == null ? "" : body.getContent().trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Tiêu đề không được để trống.");
        if (content.isEmpty()) throw new IllegalArgumentException("Nội dung không được để trống.");

        User staff = userRepo.findById(me).orElseThrow();

        News news = News.builder()
                .title(title)
                .content(content)
                .thumbnailUrl(body.getThumbnailUrl())
                .newsType(body.getNewsType())
                .isSpotlight(Boolean.TRUE.equals(body.getIsSpotlight()))
                .isDraft(false)
                .createdBy(staff)
                .club(null)
                .build();

        newsRepo.save(news);

        // Realtime
        webSocketService.broadcastSystemWide("NEWS", "PUBLISHED", newsMapper.toDto(news));

        return new PublishResult(news.getId(), newsMapper.toDto(news), "Đăng trực tiếp thành công");
    }

    // ========== CANCEL REQUEST ==========
    @Transactional
    public void cancelRequest(Long me, Long requestId) {
        RequestNews r = requestRepo.findDetailById(requestId).orElseThrow();

        RequestStatus st = r.getStatus();
        Long clubId = r.getClub() != null ? r.getClub().getId() : null;

        boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(me);
        boolean isStaff   = guard.isStaff(me);
        boolean isClubMgr = clubId != null && guard.canApproveAtClub(me, clubId);

        boolean canCancel;
        if (st == RequestStatus.PENDING_CLUB) {
            canCancel = isCreator || isClubMgr || isStaff;
        } else if (st == RequestStatus.PENDING_UNIVERSITY) {
            canCancel = isClubMgr || isStaff;
        } else {
            throw new IllegalStateException("Chỉ hủy được khi request đang ở trạng thái PENDING.");
        }

        if (!canCancel) throw new SecurityException("Không có quyền hủy request này.");

        News news = r.getNews();
        if (news != null) {
            news.setIsDraft(true);
            newsRepo.save(news);
        }

        r.setStatus(RequestStatus.CANCELED);
        requestRepo.save(r);

        // Realtime
        webSocketService.broadcastToClub(clubId, "NEWS_REQUEST", "CANCELED",
                Map.of("requestId", r.getId(), "status", r.getStatus().name()));
    }

    private String buildCreatorRequestUrl(RequestNews r) {
        Long clubId = r.getClub() != null ? r.getClub().getId() : null;

        if (r.getTeam() != null && clubId != null) {
            return "/myclub/" + clubId + "/teams/" + r.getTeam().getId() + "/news/requests/" + r.getId();
        }
        return "/myclub/" + clubId + "/news/requests/" + r.getId();
    }

}
