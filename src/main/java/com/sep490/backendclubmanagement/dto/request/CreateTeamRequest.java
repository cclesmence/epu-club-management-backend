package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTeamRequest {

    @NotNull(message = "Club ID không được để trống")
    private Long clubId;

    @NotBlank(message = "Tên ban không được để trống")
    @Size(max = 150, message = "Tên ban tối đa 150 ký tự")
    private String teamName;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 512, message = "Link nhóm tối đa 512 ký tự")
    private String linkGroupChat;

    // --- CÁC TRƯỜNG TÙY CHỌN ĐỂ GÁN VAI TRÒ ---
    private Long leaderUserId;       // Trưởng ban (optional)
    private Long viceLeaderUserId;   // Phó ban (optional)
    private List<Long> memberUserIds; // Thành viên ban (optional)
}
