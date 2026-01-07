package com.sep490.backendclubmanagement.dto.response;

import java.util.List;

public class TeamData {
    private Long id;
    private String teamName;
    private Long clubId;
    private String clubName;
    private List<PostWithRelationsData> posts;
}
