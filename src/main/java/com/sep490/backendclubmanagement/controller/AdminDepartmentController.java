package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.request.AdminDepartmentUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.AdminDepartmentResponse;
import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.admin.AdminDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    // Lấy thông tin 1 phòng ban theo id
    @GetMapping("/{id}")
    public ApiResponse<AdminDepartmentResponse> getDepartmentById(
            @PathVariable Long id
    ) throws AppException {

        AdminDepartmentResponse response = adminDepartmentService.getDepartmentById(id);
        return ApiResponse.success(response);
    }


    // Update thông tin phòng ban theo id
    @PutMapping("/{id}")
    public ApiResponse<AdminDepartmentResponse> updateDepartmentInfo(
            @PathVariable Long id,
            @RequestBody AdminDepartmentUpdateRequest request
    ) throws AppException {

        AdminDepartmentResponse updated =
                adminDepartmentService.updateDepartment(id, request);

        return ApiResponse.success(updated);
    }
}
