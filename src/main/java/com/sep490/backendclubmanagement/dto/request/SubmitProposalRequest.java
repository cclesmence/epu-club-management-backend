package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitProposalRequest {

    @NotBlank(message = "Tiêu đề đề án không được để trống")
    private String title;

    // fileUrl: URL của file đã upload trước đó (optional nếu có file)
    // Nếu không có fileUrl thì phải có file trong request
    private String fileUrl;

    // Ghi chú/nhận xét của sinh viên đi kèm khi nộp đề án (optional)
    private String comment;
}

