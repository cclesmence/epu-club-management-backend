package com.sep490.backendclubmanagement.entity;

public enum DefenseScheduleStatus {
    PROPOSED,                // SV đã đề xuất lịch bảo vệ
    CONFIRMED,               // CB đã xác nhận lịch bảo vệ
    CSVC_BOOKING_PENDING,    // Đang chờ book CSVC
    CSVC_BOOKED,             // Đã book CSVC
    COMPLETED,               // Đã hoàn thành bảo vệ
    PASSED,                  // Đã pass
    FAILED,                  // Đã fail
    PENDING                  // Đang chờ
}
