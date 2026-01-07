package com.sep490.backendclubmanagement.service.user;

import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.dto.response.UserProfileResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileResponse getUserProfile(Long userId) throws AppException;

    UserProfileResponse updateUserProfile(Long userId,
                                          UpdateUserProfileRequest request) throws AppException;
    UserProfileResponse updateUserAvatar(Long userId,
                                         MultipartFile avatarFile) throws AppException;;
}
