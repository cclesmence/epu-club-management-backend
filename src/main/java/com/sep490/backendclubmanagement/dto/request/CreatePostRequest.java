package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePostRequest {

    @NotNull(message = "clubId is required")
    private Long clubId;

    // Nếu là post team-only thì truyền teamId; nếu clubWide = true, có thể null.
    private Long teamId;

    // true = toàn CLB, false = post nội bộ team
    @NotNull
    private Boolean clubWide;

    // Optional: nếu bạn dùng both flags
    private Boolean withinClub; // có thể bỏ nếu không cần

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    // "DRAFT" | "PUBLISHED" (mặc định DRAFT)
    private String status;

    // Danh sách media đính kèm (tùy chọn)
    private List<PostMediaItem> media;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PostMediaItem {
        private String title;
        private String mediaUrl;   // bắt buộc nếu có media
        private String mediaType;  // IMAGE | VIDEO | FILE...
        private String caption;
        private Integer displayOrder; // null -> cuối
    }
}
