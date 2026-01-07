package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTeamRequest {

    @Size(max = 150, message = "Tên ban tối đa 150 ký tự")
    private String teamName;        // optional

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;     // optional

    @Size(max = 512, message = "Link nhóm tối đa 512 ký tự")
    private String linkGroupChat;   // optional
}
