package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRequest {
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long eventTypeId;
    // ClubId không cho đổi trong flow hiện tại
    private List<MultipartFile> mediaFiles; // ảnh mới (append thêm)
    private List<Long> deleteMediaIds; // IDs của media cần xóa
}
