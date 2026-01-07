package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyClubDTO {
    private Long clubId;
    private String clubName;
    private String logoUrl;
    private List<String> clubRoles; // Danh sách club roles của user trong club này
    
    // Constructor không có clubRoles để tương thích với code cũ
    public MyClubDTO(Long clubId, String clubName, String logoUrl) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.logoUrl = logoUrl;
    }
}