package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberHistoryResponse {
    private String semesterName;
    private String semesterCode; // Mã kỳ học
    private String roleName; // Vai trò trong kỳ đó
    private String roleCode; // Mã vai trò
    private Integer roleLevel; // Cấp độ vai trò (nhỏ hơn là cao hơn)
    private String teamName; // Tên ban/team trong kỳ đó
    private String teamDescription; // Mô tả ban/team
    private Integer attendanceRate; // Tỷ lệ điểm danh trong kỳ đó
    private String status; // Trạng thái trong kỳ đó
    private Boolean isActive; // Có hoạt động trong kỳ đó không
    private String startDate; // Ngày bắt đầu kỳ học
    private String endDate; // Ngày kết thúc kỳ học
    private String joinDate; // Ngày tham gia vai trò này
    private String leaveDate; // Ngày rời vai trò này (nếu có)
}