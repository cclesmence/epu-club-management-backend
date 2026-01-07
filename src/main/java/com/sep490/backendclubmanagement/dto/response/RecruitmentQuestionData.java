package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentQuestionData {
    private Long id;
    private String questionText;
    private String questionType;
    private Integer questionOrder;
    private Integer isRequired;
    private List<String> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


