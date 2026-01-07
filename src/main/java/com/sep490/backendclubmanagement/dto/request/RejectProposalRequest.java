package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectProposalRequest {
    private String reason; // Lý do từ chối (optional)
}

