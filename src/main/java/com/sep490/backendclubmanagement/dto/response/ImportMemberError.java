package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportMemberError {
    private int row;
    private String studentCode;
    private String semesterCode;
    private String message;
}

