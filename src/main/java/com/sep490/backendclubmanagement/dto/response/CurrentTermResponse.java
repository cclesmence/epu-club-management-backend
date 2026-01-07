package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTermResponse {// Tên kỳ học hiện tại
    private String semesterName;
    private String semesterCode; // Mã kỳ học
    private String roleName; // Vai trò hiện tại trong kỳ này
    private String roleCode; // Mã vai trò
    private Integer roleLevel; // Cấp độ vai trò (nhỏ hơn là cao hơn)
    private String teamName; // Tên ban/team hiện tại
    private Integer attendanceRate; // Tỷ lệ điểm danh trong kỳ này
    private String status; // Trạng thái trong kỳ này (ACTIVE, INACTIVE, etc.)
    private Boolean isActive; // Có đang hoạt động trong kỳ này không
    private String startDate; // Ngày bắt đầu kỳ học
    private String endDate; // Ngày kết thúc kỳ học
}