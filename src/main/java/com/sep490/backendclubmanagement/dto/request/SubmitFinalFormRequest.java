package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitFinalFormRequest {

    @NotBlank(message = "Tiêu đề Hồ sơ hoàn thiện không được để trống")
    private String title;

    // fileUrl: URL của file đã upload trước đó (optional nếu có file)
    // Nếu không có fileUrl thì phải có file trong request
    private String fileUrl;

    // Ghi chú/nhận xét của sinh viên khi nộp Hồ sơ hoàn thiện (optional)
    private String comment;
}

