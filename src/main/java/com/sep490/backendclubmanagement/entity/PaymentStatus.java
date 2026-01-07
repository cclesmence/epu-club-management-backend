package com.sep490.backendclubmanagement.entity;

public enum PaymentStatus {
    PENDING,    // Chờ thanh toán
    PROCESSING, // Đang xử lý
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    CANCELLED,  // Đã hủy
    EXPIRED,
    PAID// Hết hạn
}

