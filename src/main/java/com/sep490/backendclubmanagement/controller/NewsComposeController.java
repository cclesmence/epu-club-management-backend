package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.dto.response.PublishResult;
import com.sep490.backendclubmanagement.service.news.NewsComposeService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsComposeController {

    private final NewsComposeService compose;
    private final UserService userService;


    // Tạo bài và chọn hành động: draft|request|publish
    @PostMapping("/compose")
    public ApiResponse<?> compose(
            @AuthenticationPrincipal User principal,
            @RequestParam String action, // draft|request|publish
            @RequestBody CreateNewsRequest body
    ){
        Long me = userService.getIdByEmail(principal.getUsername());
        switch (action.toLowerCase()) {
            case "draft":
                NewsData d = compose.createAsDraft(me, body);
                return ApiResponse.success(d);
            case "request":
                Object req = compose.createAndSubmitRequest(me, body);
                return ApiResponse.success(req);
            case "publish":
                PublishResult r = compose.createAndPublishByStaff(me, body);
                return ApiResponse.success(r);
            default:
                throw new IllegalArgumentException("action phải là draft|request|publish");
        }
    }
}
