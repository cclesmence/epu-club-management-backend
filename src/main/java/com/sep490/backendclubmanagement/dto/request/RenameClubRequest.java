package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenameClubRequest {

    @NotBlank(message = "Tên CLB không được để trống")
    @Size(max = 100, message = "Tên CLB không được vượt quá 100 ký tự")
    private String newClubName;
}





