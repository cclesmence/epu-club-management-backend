package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);
    long countByPost_Id(Long postId);
    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
    Page<Like> findByPost_Id(Long postId, Pageable pageable);
}
