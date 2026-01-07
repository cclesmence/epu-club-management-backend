package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.BannerResponse;
import com.sep490.backendclubmanagement.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/banner")
@RequiredArgsConstructor
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping
    public ApiResponse<BannerResponse> getBannerForPublic() {
        return ApiResponse.success(bannerService.get());
    }
}
