package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;

import com.sep490.backendclubmanagement.dto.response.LikeDTO;
import com.sep490.backendclubmanagement.dto.response.ToggleLikeResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.interaction.LikeService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;     // <-- inject interface
    private final UserService userService;

    @PostMapping("/toggle")
    public ApiResponse<ToggleLikeResponse> toggle(@PathVariable Long postId) throws AppException { // 👈 thêm throws
        Long userId = userService.getCurrentUserId();  // method này throws AppException
        boolean liked = likeService.toggleLike(postId, userId);
        long count   = likeService.count(postId);
        return ApiResponse.success(ToggleLikeResponse.builder().liked(liked).count(count).build());
    }

    @GetMapping("/me")
    public ApiResponse<Boolean> likedByMe(@PathVariable Long postId) throws AppException { // 👈 thêm throws
        Long userId = userService.getCurrentUserId();
        return ApiResponse.success(likeService.isLikedByUser(postId, userId));
    }

    @GetMapping("/count")
    public ApiResponse<Long> count(@PathVariable Long postId) {
        return ApiResponse.success(likeService.count(postId));
    }

    @GetMapping
    public ApiResponse<Page<LikeDTO>> list(@PathVariable Long postId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(likeService.listLikes(postId, pageable));
    }
}
