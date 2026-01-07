package com.sep490.backendclubmanagement.entity;

public enum RequestStatus {
    DRAFT,
    PENDING_CLUB,    // Chờ xử lý
    APPROVED_CLUB,   // Đã duyệt
    REJECTED_CLUB,
    PENDING_UNIVERSITY,    // Chờ xử lý
    APPROVED_UNIVERSITY,   // Đã duyệt
    REJECTED_UNIVERSITY,// Bị từ chối
    CANCELED
}

