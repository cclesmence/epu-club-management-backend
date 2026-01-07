package com.sep490.backendclubmanagement.service.staff;

import com.sep490.backendclubmanagement.dto.request.CreateStaffRequest;
import com.sep490.backendclubmanagement.dto.request.StaffFilterRequest;
import com.sep490.backendclubmanagement.dto.response.StaffListResponse;
import com.sep490.backendclubmanagement.dto.response.StaffSummaryResponse;
import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.repository.SystemRoleRepository;
import com.sep490.backendclubmanagement.mapper.UserMapper;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffManagementService {

    private final UserRepository userRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;

    private static final String STAFF_ROLE_NAME = "STAFF";

    @Transactional
    public StaffSummaryResponse createStaff(CreateStaffRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không hợp lệ");
        }
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống");
        }

        String fullName = request.getFullName() != null ? request.getFullName().trim() : null;
        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        String studentCode = null;
        if (request.getStudentCode() != null) {
            String scTrimmed = request.getStudentCode().trim();
            if (!scTrimmed.isEmpty()) {
                if (userRepository.existsByStudentCodeIgnoreCase(scTrimmed)) {
                    throw new IllegalArgumentException("Mã đã tồn tại trong hệ thống");
                }
                studentCode = scTrimmed;
            }
        }

        String phoneNumber = null;
        if (request.getPhoneNumber() != null) {
            String phoneTrimmed = request.getPhoneNumber().trim();
            phoneNumber = phoneTrimmed.isEmpty() ? null : phoneTrimmed;
        }

        String gender = null;
        if (request.getGender() != null) {
            String genderTrimmed = request.getGender().trim();
            gender = genderTrimmed.isEmpty() ? null : genderTrimmed.toUpperCase();
        }

        SystemRole staffRole = systemRoleRepository.findByRoleName(STAFF_ROLE_NAME)
                .orElseGet(() -> systemRoleRepository.save(SystemRole.builder()
                        .roleName(STAFF_ROLE_NAME)
                        .build()));

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .gender(gender)
                .studentCode(studentCode)
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .systemRole(staffRole)
                .build();

        User saved = userRepository.save(user);
        return userMapper.toStaffSummary(saved);
    }

    public StaffListResponse getAllStaffByFilter(StaffFilterRequest request) {
        Page<User> page = userRepository.getAllStaffByFilter(request, request.getPageable("id,desc"));
        List<StaffSummaryResponse> data = userMapper.toStaffSummaries(page.getContent());

        return StaffListResponse.builder()
                .total(page.getTotalElements())
                .count(data.size())
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public StaffSummaryResponse getStaffBasicProfile(Long staffId) {
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String roleName = user.getSystemRole() != null ? user.getSystemRole().getRoleName() : null;
        if (roleName == null || !"STAFF".equalsIgnoreCase(roleName)) {
            throw new ForbiddenException("Chỉ áp dụng cho tài khoản có vai trò STAFF");
        }
        return userMapper.toStaffSummary(user);
    }

    @Transactional
    public void setStaffActive(Long staffId, boolean isActive) {
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String roleName = user.getSystemRole() != null ? user.getSystemRole().getRoleName() : null;
        if (roleName == null || !"STAFF".equalsIgnoreCase(roleName)) {
            throw new ForbiddenException("Chỉ áp dụng cho tài khoản có vai trò STAFF");
        }
        if (Boolean.valueOf(isActive).equals(user.getIsActive())) {
            return;
        }
        user.setIsActive(isActive);
        userRepository.save(user);
    }

    @Transactional
    public StaffSummaryResponse updateStaffProfile(Long staffId, UpdateUserProfileRequest request, MultipartFile avatarFile) {
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String roleName = user.getSystemRole() != null ? user.getSystemRole().getRoleName() : null;
        if (roleName == null || !"STAFF".equalsIgnoreCase(roleName)) {
            throw new ForbiddenException("Chỉ áp dụng cho tài khoản có vai trò STAFF");
        }
        if (request != null) {
            if (request.getStudentCode() != null) {
                String sc = request.getStudentCode().trim();
                request.setStudentCode(sc.isEmpty() ? null : sc);
            }
            userMapper.updateUserFromRequest(request, user);
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            CloudinaryService.UploadResult upload = cloudinaryService.uploadImage(avatarFile, "users/avatars");
            user.setAvatarUrl(upload.url());
        }
        userRepository.save(user);
        return userMapper.toStaffSummary(user);
    }


}


