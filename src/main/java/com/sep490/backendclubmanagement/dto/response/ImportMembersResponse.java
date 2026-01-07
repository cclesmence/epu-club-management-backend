package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportMembersResponse {
    private int totalRows;
    private int processedUsers;
    private int processedHistories;
    private int createdUsers;
    private int updatedUsers;
    private int createdMemberships;
    private int updatedMemberships;
    private int createdRoleMemberships;
    private int updatedRoleMemberships;
    private List<ImportMemberError> errors;
    private String summary;
}

