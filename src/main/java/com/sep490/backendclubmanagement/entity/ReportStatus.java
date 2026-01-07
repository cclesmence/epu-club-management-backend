package com.sep490.backendclubmanagement.entity;

public enum ReportStatus {
    DRAFT,          // Bản nháp
    PENDING_CLUB,    // Chờ xử lý
    REJECTED_CLUB,
    UPDATED_PENDING_CLUB,
    PENDING_UNIVERSITY,    // Chờ xử lý
    APPROVED_UNIVERSITY,   // Đã duyệt
    REJECTED_UNIVERSITY,
    RESUBMITTED_UNIVERSITY
}

