// src/main/java/com/sep490/backendclubmanagement/repository/CommentRepository.java
package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Top-level comments của một post (phân trang, mới nhất trước)
    @Query("""
           SELECT c FROM Comment c
           WHERE c.post.id = :postId
             AND c.parentComment IS NULL
             AND c.deletedAt IS NULL
           ORDER BY c.createdAt DESC, c.id DESC
           """)
    List<Comment> findTopLevelByPost(@Param("postId") Long postId, Pageable pageable);

    // Danh sách replies theo parent (cũ → mới)
    @Query("""
           SELECT c FROM Comment c
           WHERE c.parentComment.id = :parentId
             AND c.deletedAt IS NULL
           ORDER BY c.createdAt DESC, c.id DESC
           """)
    List<Comment> findReplies(@Param("parentId") Long parentId);


    /** Id các con trực tiếp (đang sống) của 1 comment */
    @Query("""
           SELECT c.id FROM Comment c
           WHERE c.parentComment.id = :parentId
             AND c.deletedAt IS NULL
           """)
    List<Long> findActiveChildIds(@Param("parentId") Long parentId);

    // Lấy một comment còn "sống"
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Comment findActiveById(@Param("id") Long id);

    // CommentRepository.java
    @Query("""
   SELECT c FROM Comment c
   WHERE c.post.id = :postId AND c.deletedAt IS NULL
   ORDER BY c.createdAt DESC, c.id DESC
""")
    List<Comment> findAllActiveByPost(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Comment c SET c.deletedAt = :time
           WHERE c.id IN :ids
           """)
    int bulkSoftDeleteByIds(@Param("ids") List<Long> ids,
                            @Param("time") LocalDateTime time);
}
