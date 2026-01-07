package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.NewsRequestResponse;
import com.sep490.backendclubmanagement.entity.RequestNews;
import com.sep490.backendclubmanagement.entity.RequestStatus;
import com.sep490.backendclubmanagement.mapper.RequestNewsMapper;
import com.sep490.backendclubmanagement.repository.RequestNewsRepository;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("api/news/requests")
@RequiredArgsConstructor
@Slf4j

public class NewsRequestQueryController {

    private final RequestNewsRepository requestRepo;
    private final UserService userService;
    private final RoleGuard guard;
    private final RequestNewsMapper mapper; // dùng MapStruct mapper

    // ===== DETAIL =====
    @GetMapping("/{id}")
    public ApiResponse<NewsRequestResponse> getDetail(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        RequestNews r = requestRepo.findDetailById(id).orElseThrow();

        Long clubId = r.getClub() == null ? null : r.getClub().getId();
        boolean allowed = guard.isStaff(me)
                || (clubId != null && guard.canApproveAtClub(me, clubId))
                || (r.getCreatedBy() != null && r.getCreatedBy().getId().equals(me));

        if (!allowed) throw new SecurityException("Bạn không có quyền xem request này.");

        return ApiResponse.success(mapper.toDto(r)); // mapper tự fill thumbnailUrl/newsType
    }

    // ===== LIST + FILTER =====
    @GetMapping
    public ApiResponse<Object> search(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long createdByUserId
    ) {
        long t0 = System.currentTimeMillis();

        Long me = userService.getIdByEmail(principal.getUsername());

        boolean isStaff = guard.isStaff(me);

        boolean isClubManager = false;
        java.util.List<Long> leadTeamIds = java.util.List.of();

        if (!isStaff && clubId != null) {
            isClubManager = guard.isClubManager(me, clubId);
            leadTeamIds = guard.findLeadTeamIdsInClub(me, clubId);
        }

        long t1 = System.currentTimeMillis();

        int p = Math.max(1, page);
        int s = Math.max(1, size);

        var pageable = PageRequest.of(
                p - 1, s,
                Sort.by("requestDate").descending().and(Sort.by("id").descending())
        );

        // Parse status string -> enum (nếu sai thì bỏ filter)
        RequestStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = RequestStatus.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                statusFilter = null;
            }
        }

        String kw = (keyword == null || keyword.isBlank())
                ? null
                : keyword.trim().toLowerCase();

        // ===== CHỌN QUERY THEO 3 CẤP + LOG CHI TIẾT =====
        Page<RequestNews> pageRs;

        if (isStaff) {
            log.warn(
                    "[NEWS_REQUEST] STAFF query | me={} | status={} | clubId={} | teamId={} | createdBy={} | keyword={}",
                    me, statusFilter, clubId, teamId, createdByUserId, kw
            );

            pageRs = requestRepo.searchForStaff(
                    statusFilter,
                    clubId,
                    teamId,
                    createdByUserId,
                    kw,
                    pageable
            );

        } else if (clubId != null && isClubManager) {
            log.warn(
                    "[NEWS_REQUEST] CLUB_MANAGER query | me={} | clubId={} | status={} | teamId={} | createdBy={} | keyword={}",
                    me, clubId, statusFilter, teamId, createdByUserId, kw
            );

            pageRs = requestRepo.searchForClubManager(
                    clubId,
                    statusFilter,
                    teamId,
                    createdByUserId,
                    kw,
                    pageable
            );

        } else if (clubId != null && !leadTeamIds.isEmpty()) {
            log.warn(
                    "[NEWS_REQUEST] TEAM_LEAD query | me={} | clubId={} | teamIds={} | status={} | createdBy={} | keyword={}",
                    me, clubId, leadTeamIds, statusFilter, createdByUserId, kw
            );

            pageRs = requestRepo.searchForTeamLead(
                    clubId,
                    leadTeamIds,
                    statusFilter,
                    createdByUserId,
                    kw,
                    pageable
            );

        } else {
            log.warn(
                    "[NEWS_REQUEST] CREATOR query (fallback) | me={} | clubId={} | status={} | teamId={} | keyword={}",
                    me, clubId, statusFilter, teamId, kw
            );

            pageRs = requestRepo.searchForCreator(
                    me,
                    clubId,
                    statusFilter,
                    teamId,
                    kw,
                    pageable
            );
        }

        long t2 = System.currentTimeMillis();

        var mapped = pageRs.getContent().stream()
                .map(mapper::toDto)
                .toList();

        long t3 = System.currentTimeMillis();

        log.warn("""
        ======== NEWS REQUEST TIMING ========
        user+guard    = {} ms
        repo+db       = {} ms
        map dto       = {} ms
        TOTAL handler = {} ms
        =====================================""",
                (t1 - t0),
                (t2 - t1),
                (t3 - t2),
                (t3 - t0)
        );

        var result = java.util.Map.of(
                "page", p,
                "size", s,
                "total", (int) pageRs.getTotalElements(),
                "count", mapped.size(),
                "data", mapped
        );
        return ApiResponse.success(result);
    }



}
