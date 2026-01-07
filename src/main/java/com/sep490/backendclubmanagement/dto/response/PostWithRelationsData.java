package com.sep490.backendclubmanagement.dto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostWithRelationsData {
    private Long id;
    private String title;
    private String content;
    private String status;
    private boolean withinClub;
    private boolean clubWide;
    private LocalDateTime createdAt;

    private  Long teamId;
    private  String teamName;
    private Long clubId;
    private String clubName;
    private Long authorId;
    private String authorName;
    private String authorAvatarUrl;

    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;

    private Long rejectedById;
    private String rejectedByName;
    private LocalDateTime rejectedAt;

    private String rejectReason;

    private List<CommentData> comments;
    private List<LikeData> likes;
    private List<PostMediaData> media;
}
