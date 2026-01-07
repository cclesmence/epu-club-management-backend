package com.sep490.backendclubmanagement.entity;

public enum TransactionStatus {
    PENDING,    // Đang chờ xử lý
    PROCESSING, // Đang xử lý
    SUCCESS,    // Thành công
    FAILED,     // Thất bại
    CANCELLED,  // Đã hủy
    REFUNDED    // Đã hoàn tiền
}

