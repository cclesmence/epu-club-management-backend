package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateDraftRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateDraftRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.news.NewsDraftService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j

@RequestMapping("/api/news/drafts")
public class NewsDraftController {

    private final NewsDraftService draftService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<NewsData> create(
            @AuthenticationPrincipal User principal,
            @RequestBody CreateDraftRequest body
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(draftService.createDraft(me, body));
    }

    @PutMapping("/{newsId}")
    public ApiResponse<NewsData> update(
            @AuthenticationPrincipal User principal,
            @PathVariable Long newsId,
            @RequestBody UpdateDraftRequest body
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(draftService.updateDraft(me, newsId, body));
    }

    @DeleteMapping("/{newsId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal User principal,
            @PathVariable Long newsId
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        draftService.deleteDraft(me, newsId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<Page<NewsData>> list(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long t0 = System.currentTimeMillis();

        Long me = userService.getIdByEmail(principal.getUsername());

        long t1 = System.currentTimeMillis();

        Page<NewsData> rs = draftService.listDrafts(me, clubId, page, size);

        long t2 = System.currentTimeMillis();

        log.warn("\n=========== NEWS DRAFT TIMING ===========\n" +
                        "getUser = {} ms\n" +
                        "listDrafts() = {} ms\n" +
                        "TOTAL = {} ms\n" +
                        "===========================================",
                (t1 - t0),
                (t2 - t1),
                (t2 - t0)
        );

        return ApiResponse.success(rs);
    }


    @PostMapping("/{newsId}/submit")
    public ApiResponse<?> submit(
            @AuthenticationPrincipal User principal,
            @PathVariable Long newsId
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(draftService.submitDraftToRequest(me, newsId));
    }

    @PutMapping("/{newsId}/publish")
    public ApiResponse<NewsData> publish(
            @AuthenticationPrincipal User principal,
            @PathVariable Long newsId
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(draftService.publishDraftByStaff(me, newsId));
    }
    @GetMapping("/{newsId}")
    public ApiResponse<NewsData> getOne(
            @AuthenticationPrincipal User principal,
            @PathVariable Long newsId
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(draftService.getDraftDetail(me, newsId));
    }

}
