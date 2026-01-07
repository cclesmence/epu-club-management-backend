package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.dto.response.UserProfileResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.user.UserProfileService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() throws AppException {
        Long currentUserId = userService.getCurrentUserId();
        return ApiResponse.success(userProfileService.getUserProfile(currentUserId));
    }
    @PutMapping("/me/info")
    public ApiResponse<UserProfileResponse> updateMyInfo(
            @RequestBody UpdateUserProfileRequest request) throws AppException {

        Long currentUserId = userService.getCurrentUserId();
        UserProfileResponse updated = userProfileService.updateUserProfile(currentUserId, request);
        return ApiResponse.success(updated);
    }
    @PutMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserProfileResponse> updateMyAvatar(
            @RequestParam("file") MultipartFile file
    ) throws AppException {

        Long currentUserId = userService.getCurrentUserId();
        UserProfileResponse updated = userProfileService.updateUserAvatar(currentUserId, file);
        return ApiResponse.success(updated);
    }


}
