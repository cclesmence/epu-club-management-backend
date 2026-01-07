package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.request.BannerUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.BannerResponse;
import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService service;

    @GetMapping
    @PreAuthorize("@clubSecurity.isAdmin()")
    public ApiResponse<BannerResponse> get() {
        return ApiResponse.success(service.get());
    }

    @PutMapping
    @PreAuthorize("@clubSecurity.isAdmin()")
    public ApiResponse<BannerResponse> update(@RequestBody BannerUpdateRequest req) {
        return ApiResponse.success(service.update(req));
    }
}
