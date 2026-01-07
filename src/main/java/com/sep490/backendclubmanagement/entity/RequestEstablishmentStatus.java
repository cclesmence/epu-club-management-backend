package com.sep490.backendclubmanagement.entity;

public enum RequestEstablishmentStatus {
    DRAFT,                      // Bản nháp
    SUBMITTED,                  // Đã gửi
    CONTACT_CONFIRMATION_PENDING,// Đang chờ xác nhận liên hệ [5 ngày]
    CONTACT_CONFIRMED,           // Đã xác nhận liên hệ
    NAME_REVISION_REQUIRED,      // Nhân viên phòng IC-PDP yêu cầu chỉnh sửa tên CLB
    CONTACT_REJECTED,            // Từ chối xác nhận liên hệ
    PROPOSAL_REQUIRED,           // Yêu cầu đề án
    PROPOSAL_SUBMITTED,          // Đã nộp đề án
    PROPOSAL_REJECTED,           // Đề án bị từ chối (cần edit lại)
    PROPOSAL_APPROVED,           // Đề án được chấp thuận
    DEFENSE_SCHEDULE_PROPOSED,   // SV đã đề xuất lịch bảo vệ (đã book EPU)
    DEFENSE_SCHEDULE_APPROVED,   // Staff đã duyệt lịch bảo vệ
    DEFENSE_SCHEDULE_REJECTED,   // Staff từ chối lịch bảo vệ (SV cần đề xuất lại)
    DEFENSE_SCHEDULED,           // Lịch bảo vệ đã được xác nhận và lên lịch
    DEFENSE_COMPLETED,           // Bảo vệ đã hoàn thành
    FEEDBACK_PROVIDED,           // Đã có feedback sau bảo vệ
    FINAL_FORM_SUBMITTED,        // Đã nộp Hồ sơ hoàn thiện
    FINAL_FORM_REVIEWED,         // Đã xem xét Hồ sơ hoàn thiện
    APPROVED,                    // Đã duyệt
    REJECTED                     // Bị từ chối
}

