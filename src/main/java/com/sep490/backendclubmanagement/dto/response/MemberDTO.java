package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    private Long userId;
    private String fullName;
    private String studentCode;
    private String email;
    private String avatarUrl;
    private LocalDate joinDate;
    private String status;
}
