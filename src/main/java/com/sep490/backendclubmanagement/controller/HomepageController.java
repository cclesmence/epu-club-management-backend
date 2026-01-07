package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.HomepageResponse;
import com.sep490.backendclubmanagement.service.HomepageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageService homepageService;

    @GetMapping
    public ApiResponse<HomepageResponse> getHomepageData() {
        HomepageResponse data = homepageService.getHomepageData();
        return ApiResponse.success(data);
    }
}
