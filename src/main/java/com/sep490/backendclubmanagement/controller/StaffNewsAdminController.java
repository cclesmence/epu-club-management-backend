package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.StaffUpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.service.admin.StaffNewsAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/staff/news")
@RequiredArgsConstructor
public class StaffNewsAdminController {

    private final StaffNewsAdminService staffNewsAdminService;

    @PatchMapping("/{id}")
    public ApiResponse<NewsData> update(@PathVariable Long id,
                                        @Valid @RequestBody StaffUpdateNewsRequest req) {
        return ApiResponse.success(staffNewsAdminService.updateNews(id, req));
    }

    @PostMapping("/{id}/hide")
    public ApiResponse<Void> hide(@PathVariable Long id) {
        staffNewsAdminService.hide(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/unhide")
    public ApiResponse<Void> unhide(@PathVariable Long id) {
        staffNewsAdminService.unhide(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> softDelete(@PathVariable Long id) {
        staffNewsAdminService.softDelete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        staffNewsAdminService.restore(id);
        return ApiResponse.success(null);
    }
}
