package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateSemesterRequest;
import com.sep490.backendclubmanagement.dto.request.SemesterFilterRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateSemesterRequest;
import com.sep490.backendclubmanagement.dto.response.SemesterListResponse;
import com.sep490.backendclubmanagement.dto.response.SemesterSummaryResponse;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.semester.SemesterManagementService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/semester")
@RequiredArgsConstructor
public class SemesterManagementController {

    private final SemesterManagementService semesterManagementService;
    private final RoleService roleService;

    @PostMapping("/all-by-filter")
    public ApiResponse<SemesterListResponse> getAllSemestersByFilter(@RequestBody SemesterFilterRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(semesterManagementService.getAllSemestersByFilter(request));
    }

    @PostMapping("/create")
    public ApiResponse<SemesterSummaryResponse> createSemester(@RequestBody CreateSemesterRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(semesterManagementService.createSemester(request));
    }

    @PostMapping("/{semesterId}/update")
    public ApiResponse<SemesterSummaryResponse> updateSemester(@PathVariable Long semesterId,
                                                               @RequestBody UpdateSemesterRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(semesterManagementService.updateSemester(semesterId, request));
    }

    @DeleteMapping("/{semesterId}")
    public ApiResponse<Void> deleteSemester(@PathVariable Long semesterId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        semesterManagementService.deleteSemester(semesterId);
        return ApiResponse.success();
    }
}

