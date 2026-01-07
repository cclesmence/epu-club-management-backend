package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePostRequest {

    @NotNull
    private Long clubId;          // theo quy ước hiện tại của bạn

    private Long teamId;          // null nếu clubWide=true
    private Boolean clubWide;     // null => giữ nguyên
    private Boolean withinClub;   // null => giữ nguyên

    private String title;         // null => giữ nguyên
    private String content;       // null => giữ nguyên
    private String status;        // null => giữ nguyên

    // Media cũ muốn xóa (chỉ xóa bản ghi DB, KHÔNG gọi Cloudinary)
    private List<Long> deleteMediaIds;

    // Meta cho các file ảnh mới (theo thứ tự mảng files[])
    private List<NewMediaMeta> newMediasMeta;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NewMediaMeta {
        private String title;
        private String caption;
        private Integer displayOrder;
        private String mediaType;  // mặc định "IMAGE"
    }
}
