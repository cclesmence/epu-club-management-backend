package com.sep490.backendclubmanagement.service.interaction;
import com.sep490.backendclubmanagement.dto.response.LikeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeService {
    /** Toggle like: chưa like -> like; đã like -> unlike. Trả về trạng thái SAU toggle. */
    boolean toggleLike(Long postId, Long userId);

    /** Đếm số like của post. */
    long count(Long postId);

    /** User này đã like post chưa. */
    boolean isLikedByUser(Long postId, Long userId);

    /** Danh sách người đã like (phân trang). */
    Page<LikeDTO> listLikes(Long postId, Pageable pageable);
}