package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.RequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class NewsRequestResponse {
    private Long id;
    private Long clubId;
    private Long createdByUserId;
    private String requestTitle;
    private String description;
    private String responseMessage;
    private RequestStatus status;
    private LocalDateTime requestDate;
    private Long newsId;

    private String createdByFullName;
    private String createdByEmail;
    private String createdByAvatarUrl;
    private String createdByStudentCode;

    private String clubName;
    private String clubCode;
    private String clubLogoUrl;

    private Long teamId;
    private String teamName;

    private String thumbnailUrl; // NEW
    private String newsType;     // NEW
}
