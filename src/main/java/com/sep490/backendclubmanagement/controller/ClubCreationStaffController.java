package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.AssignRequestEstablishmentRequest;
import com.sep490.backendclubmanagement.dto.request.CompleteDefenseRequest;
import com.sep490.backendclubmanagement.dto.request.RejectContactRequest;
import com.sep490.backendclubmanagement.dto.request.RequestProposalRequest;
import com.sep490.backendclubmanagement.dto.request.RejectDefenseScheduleRequest;
import com.sep490.backendclubmanagement.dto.request.RejectProposalRequest;
import com.sep490.backendclubmanagement.dto.request.RequestNameRevisionRequest;
import com.sep490.backendclubmanagement.dto.response.ClubCreationFinalFormResponse;
import com.sep490.backendclubmanagement.dto.response.ClubProposalResponse;
import com.sep490.backendclubmanagement.dto.response.DefenseScheduleResponse;
import com.sep490.backendclubmanagement.dto.response.RequestEstablishmentResponse;
import com.sep490.backendclubmanagement.dto.response.WorkflowHistoryResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.RequestEstablishmentService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/club-creation/requests")
@RequiredArgsConstructor
public class ClubCreationStaffController {

    private final RequestEstablishmentService requestEstablishmentService;
    private final RoleService roleService;

    /**
     * Xem danh sách yêu cầu chờ xử lý
     * GET /api/staff/club-creation/requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<RequestEstablishmentResponse>>> getPendingRequests(
            @PageableDefault(size = 10) Pageable pageable
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        Page<RequestEstablishmentResponse> responses = requestEstablishmentService.getPendingRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Xem chi tiết yêu cầu
     * GET /api/staff/club-creation/requests/{requestId}
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> getRequestDetail(
            @PathVariable Long requestId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.getRequestDetailForStaff(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gán Nhân viên phòng IC-PDP xử lý yêu cầu (tự nhận hoặc gán cho Nhân viên phòng IC-PDP khác)
     * POST /api/staff/club-creation/requests/{requestId}/assign
     */
    @PostMapping("/{requestId}/assign")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> assignRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) AssignRequestEstablishmentRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        if (request == null) {
            request = new AssignRequestEstablishmentRequest();
        }
        RequestEstablishmentResponse response = requestEstablishmentService.assignRequest(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Nhân viên phòng IC-PDP nhận yêu cầu và set deadline 5 ngày
     * POST /api/staff/club-creation/requests/{requestId}/receive
     */
    @PostMapping("/{requestId}/receive")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> receiveRequest(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.receiveRequest(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xác nhận liên hệ
     * POST /api/staff/club-creation/requests/{requestId}/contact/confirm
     */
    @PostMapping("/{requestId}/contact/confirm")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> confirmContact(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.confirmContact(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Từ chối xác nhận liên hệ
     * POST /api/staff/club-creation/requests/{requestId}/contact/reject
     */
    @PostMapping("/{requestId}/contact/reject")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> rejectContact(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectContactRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.rejectContact(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Yêu cầu SV điền đề án
     * POST /api/staff/club-creation/requests/{requestId}/request-proposal
     */
    @PostMapping("/{requestId}/request-proposal")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> requestProposal(
            @PathVariable Long requestId,
            @RequestBody(required = false) RequestProposalRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.requestProposal(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách đề án của một request
     * GET /api/staff/club-creation/requests/{requestId}/proposals
     */
    @GetMapping("/{requestId}/proposals")
    public ResponseEntity<ApiResponse<List<ClubProposalResponse>>> getProposals(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        List<ClubProposalResponse> proposals = requestEstablishmentService.getProposalsForStaff(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * Xem danh sách Hồ sơ hoàn thiện đã nộp của một request (dành cho Nhân viên phòng IC-PDP được giao)
     * GET /api/staff/club-creation/requests/{requestId}/final-forms
     */
    @GetMapping("/{requestId}/final-forms")
    public ResponseEntity<ApiResponse<List<ClubCreationFinalFormResponse>>> getFinalForms(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        List<ClubCreationFinalFormResponse> responses = requestEstablishmentService.getFinalFormsForStaff(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Duyệt Hồ sơ hoàn thiện và tự động tạo CLB
     * POST /api/staff/club-creation/requests/{requestId}/final-forms/approve
     */
    @PostMapping("/{requestId}/final-forms/approve")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> approveFinalForm(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.approveFinalForm(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách đề án đã nộp (status = PROPOSAL_SUBMITTED)
     * GET /api/staff/club-creation/requests/submitted-proposals
     */
    @GetMapping("/submitted-proposals")
    public ResponseEntity<ApiResponse<Page<ClubProposalResponse>>> getSubmittedProposals(
            @PageableDefault(size = 10) Pageable pageable
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        Page<ClubProposalResponse> proposals = requestEstablishmentService.getSubmittedProposals(staffId, pageable);
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * Xem chi tiết đề án
     * GET /api/staff/club-creation/requests/{requestId}/proposals/{proposalId}
     */
    @GetMapping("/{requestId}/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ClubProposalResponse>> getProposalDetail(
            @PathVariable Long requestId,
            @PathVariable Long proposalId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        ClubProposalResponse proposal = requestEstablishmentService.getProposalDetailForStaff(requestId, proposalId, staffId);
        return ResponseEntity.ok(ApiResponse.success(proposal));
    }

    /**
     * Duyệt đề án (chuyển PROPOSAL_SUBMITTED → PROPOSAL_APPROVED)
     * POST /api/staff/club-creation/requests/{requestId}/proposals/approve
     */
    @PostMapping("/{requestId}/proposals/approve")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> approveProposal(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.approveProposal(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Từ chối đề án (chuyển PROPOSAL_SUBMITTED → PROPOSAL_REJECTED)
     * POST /api/staff/club-creation/requests/{requestId}/proposals/reject
     */
    @PostMapping("/{requestId}/proposals/reject")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> rejectProposal(
            @PathVariable Long requestId,
            @RequestBody(required = false) RejectProposalRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        if (request == null) {
            request = new RejectProposalRequest();
        }
        RequestEstablishmentResponse response = requestEstablishmentService.rejectProposal(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem danh sách lịch bảo vệ đã đề xuất (status = DEFENSE_SCHEDULE_PROPOSED)
     * GET /api/staff/club-creation/requests/proposed-defense-schedules
     */
    @GetMapping("/proposed-defense-schedules")
    public ResponseEntity<ApiResponse<Page<DefenseScheduleResponse>>> getProposedDefenseSchedules(
            @PageableDefault(size = 10) Pageable pageable
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        Page<DefenseScheduleResponse> schedules = requestEstablishmentService.getProposedDefenseSchedules(staffId, pageable);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    /**
     * Xem chi tiết lịch bảo vệ
     * GET /api/staff/club-creation/requests/{requestId}/defense-schedule
     */
    @GetMapping("/{requestId}/defense-schedule")
    public ResponseEntity<ApiResponse<DefenseScheduleResponse>> getDefenseSchedule(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        DefenseScheduleResponse schedule = requestEstablishmentService.getDefenseScheduleForStaff(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    /**
     * Duyệt lịch bảo vệ (chuyển DEFENSE_SCHEDULE_PROPOSED → DEFENSE_SCHEDULE_APPROVED)
     * POST /api/staff/club-creation/requests/{requestId}/defense-schedule/approve
     */
    @PostMapping("/{requestId}/defense-schedule/approve")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> approveDefenseSchedule(
            @PathVariable Long requestId
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.approveDefenseSchedule(requestId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Từ chối lịch bảo vệ (chuyển DEFENSE_SCHEDULE_PROPOSED → DEFENSE_SCHEDULE_REJECTED)
     * POST /api/staff/club-creation/requests/{requestId}/defense-schedule/reject
     */
    @PostMapping("/{requestId}/defense-schedule/reject")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> rejectDefenseSchedule(
            @PathVariable Long requestId,
            @RequestBody(required = false) RejectDefenseScheduleRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        if (request == null) {
            request = new RejectDefenseScheduleRequest();
        }
        RequestEstablishmentResponse response = requestEstablishmentService.rejectDefenseSchedule(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Nhập kết quả bảo vệ (PASSED/FAILED) + feedback
     * POST /api/staff/club-creation/requests/{requestId}/defense/complete
     */
    @PostMapping("/{requestId}/defense/complete")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> completeDefense(
            @PathVariable Long requestId,
            @Valid @RequestBody CompleteDefenseRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.completeDefense(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xem lịch sử workflow của yêu cầu
     * GET /api/staff/club-creation/requests/{requestId}/history
     */
    @GetMapping("/{requestId}/history")
    public ResponseEntity<ApiResponse<Page<WorkflowHistoryResponse>>> getWorkflowHistory(
            @PathVariable Long requestId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        Page<WorkflowHistoryResponse> histories = requestEstablishmentService.getWorkflowHistory(requestId, pageable);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * Nhân viên phòng IC-PDP yêu cầu sinh viên chỉnh sửa tên CLB
     * POST /api/staff/club-creation/requests/{requestId}/request-name-revision
     */
    @PostMapping("/{requestId}/request-name-revision")
    public ResponseEntity<ApiResponse<RequestEstablishmentResponse>> requestNameRevision(
            @PathVariable Long requestId,
            @RequestBody(required = false) RequestNameRevisionRequest request
    ) throws AppException {
        Long staffId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(staffId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        RequestEstablishmentResponse response = requestEstablishmentService.requestNameRevision(requestId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

