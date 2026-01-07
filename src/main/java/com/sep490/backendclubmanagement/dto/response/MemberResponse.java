package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    // Thông tin cơ bản từ User entity
    private Long userId; // ID người dùng
    private String studentCode; // MSSV
    private String fullName; // Họ tên
    private String email; // Email
    private String phoneNumber; // Số điện thoại
    private String avatarUrl; // URL avatar
    private String gender; // Giới tính
    private String dateOfBirth; // Ngày sinh
    
    // Thông tin từ ClubMemberShip entity
    private String clubName; // Tên CLB
    private String clubCode; // Mã CLB
    private String membershipStatus; // Trạng thái thành viên (ACTIVE, LEFT)
    private String joinDate; // Ngày tham gia CLB
    private String endDate; // Ngày rời CLB (nếu có)
    
    // Thông tin tổng hợp
    private Integer totalAttendanceRate; // Tỷ lệ điểm danh trung bình
    private Integer totalTerms; // Tổng số kỳ đã tham gia
    private String lastActive; // Hoạt động cuối cùng
    
    // Thông tin kỳ hiện tại
    private CurrentTermResponse currentTerm;
    
    // Lịch sử các kỳ học
    private List<MemberHistoryResponse> history;
}
