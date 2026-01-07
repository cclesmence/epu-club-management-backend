package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRequestEstablishmentRequest {
    private Long staffId; // null = tự nhận, có giá trị = gán cho staff khác
}

