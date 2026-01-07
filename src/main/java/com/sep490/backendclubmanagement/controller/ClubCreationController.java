package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateRequestEstablishmentRequest;
import com.sep490.backendclubmanagement.dto.request.ProposeDefenseScheduleRequest;
import com.sep490.backendclubmanagement.dto.request.RenameClubRequest;
import com.sep490.backendclubmanagement.dto.request.SubmitFinalFormRequest;
import com.sep490.backendclubmanagement.dto.request.SubmitProposalRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateRequestEstablishmentRequest;
import com.sep490.backendclubmanagement.dto.response.ClubCreationFinalFormResponse;
import com.sep490.backendclubmanagement.dto.response.ClubCreationStepResponse;
import com.sep490.backendclubmanagement.dto.response.ClubProposalResponse;
import com.sep490.backendclubmanagement.dto.response.DefenseScheduleResponse;
import com.sep490.backendclubmanagement.dto.response.RequestEstablishmentResponse;
import com.sep490.backendclubmanagement.dto.response.WorkflowHistoryResponse;
import com.sep490.backendclubmanagement.entity.RequestEstablishmentStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.RequestEstablishmentService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/club-creation/requests")
@RequiredArgsConstructor
public class ClubCreationController {

    private final RequestEstablishmentService requestEstablishmentService;

    /**
     * Tạo đề nghị thành lập CLB mới
     * POST /api/club-creation/requests
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> createRequest(
            @Valid @RequestBody CreateRequestEstablishmentRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RequestEstablishmentResponse response = requestEstablishmentService.createRequest(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách yêu cầu của mình
     * GET /api/club-creation/requests
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RequestEstablishmentResponse>>> getMyRequests(
            @RequestParam(required = false) RequestEstablishmentStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<RequestEstablishmentResponse> responses = requestEstablishmentService.getMyRequests(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Xem chi tiết yêu cầu của mình
     * GET /api/club-creation/requests/{requestId}
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> getRequestDetail(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RequestEstablishmentResponse response = requestEstablishmentService.getRequestDetail(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Cập nhật yêu cầu (chỉ khi status = DRAFT)
     * PUT /api/club-creation/requests/{requestId}
     */
    @PutMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> updateRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestEstablishmentRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RequestEstablishmentResponse response = requestEstablishmentService.updateRequest(requestId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xóa yêu cầu (chỉ khi status = DRAFT)
     * DELETE /api/club-creation/requests/{requestId}
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        requestEstablishmentService.deleteRequest(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * Gửi yêu cầu (chuyển từ DRAFT → SUBMITTED)
     * POST /api/club-creation/requests/{requestId}/submit
     */
    @PostMapping("/{requestId}/submit")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> submitRequest(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RequestEstablishmentResponse response = requestEstablishmentService.submitRequest(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @PostMapping(value = "/{requestId}/proposal", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> submitProposal(
            @PathVariable Long requestId,
            @RequestPart("title") String title,
            @RequestPart(value = "fileUrl", required = false) String fileUrl,
            @RequestPart(value = "comment", required = false) String comment,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            throw new AppException(com.sep490.backendclubmanagement.exception.ErrorCode.INVALID_INPUT, "Tiêu đề đề án không được để trống");
        }
        
        // Build request object
        SubmitProposalRequest request = new SubmitProposalRequest();
        request.setTitle(title.trim());
        request.setFileUrl(fileUrl);
        request.setComment(comment);
        
        RequestEstablishmentResponse response = requestEstablishmentService.submitProposal(requestId, userId, request, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách đề án của yêu cầu
     * GET /api/club-creation/requests/{requestId}/proposals
     */
    @GetMapping("/{requestId}/proposals")
    public ResponseEntity<ApiResponse<List<ClubProposalResponse>>> getProposals(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ClubProposalResponse> proposals = requestEstablishmentService.getProposals(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * Xem chi tiết đề án
     * GET /api/club-creation/requests/{requestId}/proposals/{proposalId}
     */
    @GetMapping("/{requestId}/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ClubProposalResponse>> getProposalDetail(
            @PathVariable Long requestId,
            @PathVariable Long proposalId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubProposalResponse proposal = requestEstablishmentService.getProposalDetail(requestId, proposalId, userId);
        return ResponseEntity.ok(ApiResponse.success(proposal));
    }

    /**
     * Đề xuất lịch bảo vệ
     * POST /api/club-creation/requests/{requestId}/defense-schedule/propose
     */
    @PostMapping("/{requestId}/defense-schedule/propose")
    public ResponseEntity<ApiResponse<DefenseScheduleResponse>> proposeDefenseSchedule(
            @PathVariable Long requestId,
            @Valid @RequestBody ProposeDefenseScheduleRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        DefenseScheduleResponse response = requestEstablishmentService.proposeDefenseSchedule(requestId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem lịch bảo vệ
     * GET /api/club-creation/requests/{requestId}/defense-schedule
     */
    @GetMapping("/{requestId}/defense-schedule")
    public ResponseEntity<ApiResponse<DefenseScheduleResponse>> getDefenseSchedule(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        DefenseScheduleResponse response = requestEstablishmentService.getDefenseSchedule(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Cập nhật lịch bảo vệ (nếu chưa được confirm)
     * PUT /api/club-creation/requests/{requestId}/defense-schedule
     */
    @PutMapping("/{requestId}/defense-schedule")
    public ResponseEntity<ApiResponse<DefenseScheduleResponse>> updateDefenseSchedule(
            @PathVariable Long requestId,
            @Valid @RequestBody ProposeDefenseScheduleRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        DefenseScheduleResponse response = requestEstablishmentService.updateDefenseSchedule(requestId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Nộp Hồ sơ hoàn thiện (chuyển từ DEFENSE_COMPLETED → FINAL_FORM_SUBMITTED)
     * POST /api/club-creation/requests/{requestId}/final-form
     * 
     * Hỗ trợ upload file trực tiếp (Word, Excel, PDF) hoặc dùng fileUrl
     * Content-Type: multipart/form-data
     * 
     * Form data:
     * - title: String (required)
     * - fileUrl: String (optional, nếu không có file)
     * - file: MultipartFile (optional, nếu không có fileUrl)
     */
    @PostMapping(value = "/{requestId}/final-form", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ClubCreationFinalFormResponse>> submitFinalForm(
            @PathVariable Long requestId,
            @RequestParam("title") String title,
            @RequestParam(value = "fileUrl", required = false) String fileUrl,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            throw new AppException(com.sep490.backendclubmanagement.exception.ErrorCode.INVALID_INPUT, "Tiêu đề Hồ sơ hoàn thiện không được để trống");
        }
        
        // Build request object
        SubmitFinalFormRequest request = new SubmitFinalFormRequest();
        request.setTitle(title.trim());
        request.setFileUrl(fileUrl);
        request.setComment(comment);
        
        ClubCreationFinalFormResponse response = requestEstablishmentService.submitFinalForm(requestId, userId, request, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách Hồ sơ hoàn thiện đã nộp (tất cả version)
     * GET /api/club-creation/requests/{requestId}/final-forms
     */
    @GetMapping("/{requestId}/final-forms")
    public ResponseEntity<ApiResponse<List<ClubCreationFinalFormResponse>>> getFinalForms(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ClubCreationFinalFormResponse> responses = requestEstablishmentService.getFinalFormsForStudent(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Xem lịch sử workflow của yêu cầu
     * GET /api/club-creation/requests/{requestId}/history
     */
    @GetMapping("/{requestId}/history")
    public ResponseEntity<ApiResponse<Page<WorkflowHistoryResponse>>> getWorkflowHistory(
            @PathVariable Long requestId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        // Check permission: only creator can view history
        requestEstablishmentService.getRequestDetail(requestId, userId);
        Page<WorkflowHistoryResponse> histories = requestEstablishmentService.getWorkflowHistory(requestId, pageable);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * Lấy danh sách tất cả các bước trong quy trình tạo CLB
     * GET /api/club-creation/requests/steps
     */
    @GetMapping("/steps")
    public ResponseEntity<ApiResponse<List<ClubCreationStepResponse>>> getAllSteps() throws AppException {
        List<ClubCreationStepResponse> steps = requestEstablishmentService.getAllSteps();
        return ResponseEntity.ok(ApiResponse.success(steps));
    }

    /**
     * Sinh viên cập nhật lại tên CLB theo yêu cầu của staff
     * PUT /api/club-creation/requests/{requestId}/rename
     */
    @PutMapping("/{requestId}/rename")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> submitNameRevision(
            @PathVariable Long requestId,
            @Valid @RequestBody RenameClubRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RequestEstablishmentResponse response = requestEstablishmentService.submitNameRevision(requestId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

