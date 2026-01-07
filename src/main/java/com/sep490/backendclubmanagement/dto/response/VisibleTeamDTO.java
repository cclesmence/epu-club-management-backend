package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisibleTeamDTO {
    private Long teamId;
    private String teamName;
    private String description;
    private Long memberCount;       // tổng thành viên (distinct)
    private List<String> myRoles;   // vai trò của current user trong team này (nếu có)
}
