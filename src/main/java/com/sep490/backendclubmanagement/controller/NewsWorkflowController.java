// src/main/java/com/sep490/backendclubmanagement/controller/NewsWorkflowController.java
package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.ApproveNewsRequest;
import com.sep490.backendclubmanagement.dto.request.CreateNewsRequest;
import com.sep490.backendclubmanagement.dto.request.RejectNewsRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsRequestResponse;
import com.sep490.backendclubmanagement.dto.response.PublishResult;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.news.NewsWorkflowService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/news")
@RequiredArgsConstructor
public class NewsWorkflowController {

    private final NewsWorkflowService workflow;
    private final UserService userService;

    // CREATE REQUEST
    @PostMapping("/requests")
    public ApiResponse<NewsRequestResponse> createRequest(
            @AuthenticationPrincipal User principal,
            @RequestBody CreateNewsRequest body
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.createRequest(me, body));
    }

    // (NEW) UPDATE REQUEST WHEN PENDING (role-restricted)
    @PutMapping("/requests/{id}")
    public ApiResponse<NewsRequestResponse> updatePendingRequest(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id,
            @RequestBody UpdateNewsRequest body
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.updatePendingRequest(me, id, body));
    }

    // CLUB approve & submit to staff
    @PutMapping("/requests/{id}/club/approve-submit")
    public ApiResponse<NewsRequestResponse> clubApproveAndSubmit(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id,
            @RequestBody(required = false) ApproveNewsRequest body
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.clubApproveAndSubmit(me, id, body));
    }

    // CLUB president reject
    @PutMapping("/requests/{id}/club/president-reject")
    public ApiResponse<NewsRequestResponse> clubPresidentReject(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id,
            @RequestBody RejectNewsRequest body
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.clubPresidentReject(me, id, body));
    }

    // STAFF approve & publish
    @PutMapping("/requests/{id}/staff/approve-publish")
    public ApiResponse<NewsRequestResponse> staffApprovePublish(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.staffApproveAndPublish(me, id, null));
    }

    // STAFF reject
    @PutMapping("/requests/{id}/staff/reject")
    public ApiResponse<NewsRequestResponse> staffReject(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id,
            @RequestBody RejectNewsRequest body
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.staffReject(me, id, body));
    }

    // STAFF direct publish
    @PostMapping("/staff/direct-publish")
    public ApiResponse<PublishResult> staffDirectPublish(
            @AuthenticationPrincipal User principal,
            @RequestBody ApproveNewsRequest body
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(workflow.staffDirectPublish(me, body));
    }

    // Cancel request (siết quyền theo trạng thái)
    @PutMapping("/requests/{id}/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id
    ){
        Long me = userService.getIdByEmail(principal.getUsername());
        workflow.cancelRequest(me, id);
        return ApiResponse.success(null);
    }
}

//    @GetMapping("/requests")
//    public ApiResponse<List<NewsRequestResponse>> listRequests(
//            @RequestParam(required = false) RequestStatus status
//    ){
//        var list = requestRepo.findByStatus(status).stream().map(r ->
//                NewsRequestResponse.builder()
//                        .id(r.getId())
//                        .clubId(r.getClub() == null ? null : r.getClub().getId())
//                        .createdByUserId(r.getCreatedBy() == null ? null : r.getCreatedBy().getId())
//                        .requestTitle(r.getRequestTitle())
//                        .description(r.getDescription())
//                        .responseMessage(r.getResponseMessage())
//                        .status(r.getStatus())
//                        .requestDate(r.getRequestDate())
//                        .newsId(r.getNews() == null ? null : r.getNews().getId())
//                        .build()
//        ).toList();
//        return ApiResponse.success(list);
//    }

