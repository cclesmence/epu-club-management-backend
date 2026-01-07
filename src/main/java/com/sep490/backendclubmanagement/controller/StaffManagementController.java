package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateStaffRequest;
import com.sep490.backendclubmanagement.dto.request.StaffActiveRequest;
import com.sep490.backendclubmanagement.dto.request.StaffFilterRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.dto.response.StaffListResponse;
import com.sep490.backendclubmanagement.dto.response.StaffSummaryResponse;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.staff.StaffManagementService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class StaffManagementController {

    private final StaffManagementService staffManagementService;
    private final RoleService roleService;


    @PostMapping("/staff/get-all-by-filter")
    public ApiResponse<StaffListResponse> getAllStaffByFilter(@RequestBody StaffFilterRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(userId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(staffManagementService.getAllStaffByFilter(request));
    }

    @PostMapping("/staff/create")
    public ApiResponse<StaffSummaryResponse> createStaff(@RequestBody CreateStaffRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(userId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(staffManagementService.createStaff(request));
    }

    @GetMapping("/staff/{staffId}")
    public ApiResponse<StaffSummaryResponse> getStaffProfile(@PathVariable Long staffId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(userId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(staffManagementService.getStaffBasicProfile(staffId));
    }


    @PostMapping("/staff/{staffId}/active")
    public ApiResponse<Void> setStaffActive(@PathVariable Long staffId, @RequestBody StaffActiveRequest body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(userId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        boolean isActive = body != null && body.getIsActive() != null ? body.getIsActive() : true;
        staffManagementService.setStaffActive(staffId, isActive);
        return ApiResponse.success();
    }

    @PostMapping(value = "/staff/{staffId}/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StaffSummaryResponse> updateStaffProfile(@PathVariable Long staffId,
                                                                @ModelAttribute UpdateUserProfileRequest request,
                                                                @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(userId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(staffManagementService.updateStaffProfile(staffId, request, avatarFile));
    }

}
