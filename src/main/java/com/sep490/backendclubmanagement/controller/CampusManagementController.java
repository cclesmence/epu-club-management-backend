package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CampusFilterRequest;
import com.sep490.backendclubmanagement.dto.request.CreateCampusRequest;
import com.sep490.backendclubmanagement.dto.response.CampusListResponse;
import com.sep490.backendclubmanagement.dto.response.CampusSummaryResponse;
import com.sep490.backendclubmanagement.dto.request.UpdateCampusRequest;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.campus.CampusManagementService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/campus")
@RequiredArgsConstructor
public class CampusManagementController {

    private final CampusManagementService campusManagementService;
    private final RoleService roleService;

    @PostMapping("/all-by-filter")
    public ApiResponse<CampusListResponse> getAllCampusesByFilter(@RequestBody CampusFilterRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(campusManagementService.getAllCampusesByFilter(request));
    }

    @PostMapping("/create")
    public ApiResponse<CampusSummaryResponse> createCampus(@RequestBody CreateCampusRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(campusManagementService.createCampus(request));
    }

    @PostMapping("/{campusId}/update")
    public ApiResponse<CampusSummaryResponse> updateCampus(@PathVariable Long campusId,
                                                           @RequestBody UpdateCampusRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        return ApiResponse.success(campusManagementService.updateCampus(campusId, request));
    }

    @DeleteMapping("/{campusId}")
    public ApiResponse<Void> deleteCampus(@PathVariable Long campusId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!roleService.isAdmin(currentUserId)) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền truy cập");
        }
        campusManagementService.deleteCampus(campusId);
        return ApiResponse.success();
    }

}


