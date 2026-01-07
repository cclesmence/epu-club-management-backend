package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyTeamDetailDTO {
    private Long teamId;
    private String teamName;
    private String description;
    private boolean member;       // user hiện tại có thuộc team không
    private List<String> myRoles;   // vai trò của user trong team
    private Long memberCount;// tổng thành viên (distinct) của team
    private List<TeamMemberDTO> members;
    private String linkGroupChat;
}
