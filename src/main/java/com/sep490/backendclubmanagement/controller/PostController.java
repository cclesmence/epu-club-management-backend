package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreatePostRequest;
import com.sep490.backendclubmanagement.dto.request.UpdatePostRequest;
import com.sep490.backendclubmanagement.dto.response.PostWithRelationsData;
import com.sep490.backendclubmanagement.entity.NotificationPriority;
import com.sep490.backendclubmanagement.entity.NotificationType;
import com.sep490.backendclubmanagement.entity.Post;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.repository.PostRepository;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.post.PostService;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.util.PostStatus;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {
    private final PostRepository postRepository;
    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final EntityManager entityManager;
    private final NotificationService notificationService;


    // 1) Bài toàn CLB (club-wide)
    // GET /posts/{clubId}/club-wide?Pageable...
    @GetMapping("/{clubId}/club-wide")
    public ApiResponse<Page<PostWithRelationsData>> getClubWidePosts(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PostWithRelationsData> data = postService.getClubWidePosts(clubId, pageable);
        return ApiResponse.success(data);
    }

    // 2) Bài theo team trong CLB
    // GET /posts/{clubId}/teams/{teamId}?Pageable...
    @GetMapping("/{clubId}/teams/{teamId}")
    public ApiResponse<Page<PostWithRelationsData>> getTeamPosts(
            @PathVariable Long clubId,
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PostWithRelationsData> data = postService.getTeamPosts(clubId, teamId, pageable);
        return ApiResponse.success(data);
    }

    // 2.1) Bài chờ duyệt toàn CLB (club-wide pending)
    // GET /posts/{clubId}/club-wide/pending?Pageable...
    @GetMapping("/{clubId}/club-wide/pending")
    public ApiResponse<Page<PostWithRelationsData>> getPendingClubWidePosts(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PostWithRelationsData> data = postService.getPendingClubWidePosts(clubId, pageable);
        return ApiResponse.success(data);
    }

    // 2.2) Bài chờ duyệt theo team
    // GET /posts/{clubId}/teams/{teamId}/pending?Pageable...
    @GetMapping("/{clubId}/teams/{teamId}/pending")
    public ApiResponse<Page<PostWithRelationsData>> getPendingTeamPosts(
            @PathVariable Long clubId,
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PostWithRelationsData> data = postService.getPendingTeamPosts(clubId, teamId, pageable);
        return ApiResponse.success(data);
    }
    // GET /api/posts/{clubId}/feed?page=0&size=10&sort=createdAt,desc
    @GetMapping("/{clubId}/feed")
    public ApiResponse<Page<PostWithRelationsData>> getClubFeed(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) throws Exception {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Long userId = userService.getCurrentUserId();
        Page<PostWithRelationsData> data = postService.getClubFeed(clubId, userId, pageable);
        return ApiResponse.success(data);
    }

@PostMapping(path = "/create/with-media", consumes = "multipart/form-data")
public ApiResponse<PostWithRelationsData> createPostWithMedia(
        @RequestPart("request") String reqJson,                    // 👈 nhận String
        @RequestPart(value = "files", required = false) List<MultipartFile> files
) throws Exception {
    CreatePostRequest req = objectMapper.readValue(reqJson, CreatePostRequest.class); // 👈 tự parse
    Long authorId = userService.getCurrentUserId();
    return ApiResponse.success(postService.createPostWithUploads(req, files, authorId));
}

@PutMapping(path = "/update/{postId}", consumes = "multipart/form-data")
public ApiResponse<PostWithRelationsData> updatePost(
        @PathVariable Long postId,
        @RequestPart("request") String reqJson, // đổi sang String
        @RequestPart(value = "files", required = false) List<MultipartFile> files
) throws Exception {
    UpdatePostRequest req = objectMapper.readValue(reqJson, UpdatePostRequest.class);
    Long authorId = userService.getCurrentUserId();
    var data = postService.updatePostWithUploads(postId, req, files, authorId);
    return ApiResponse.success(data);
}

    @DeleteMapping("/delete/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ApiResponse.success(null);
    }
    @DeleteMapping("/delete/{postId}/media/{mediaId}")
    public ApiResponse<PostWithRelationsData> deleteOneMedia(
            @PathVariable Long postId,
            @PathVariable Long mediaId
    ) {
        var data = postService.deleteOneMedia(postId, mediaId);
        return ApiResponse.success(data);
    }


    @PostMapping("/{postId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long postId) throws AppException {
        Long approverId = userService.getCurrentUserId();
        Post p = postRepository.findById(postId).orElseThrow();

        if (!postService.canApprove(approverId, p)) {
            throw new IllegalArgumentException("Bạn không có quyền duyệt bài này");
        }

        p.setStatus(PostStatus.PUBLISHED);
        p.setApprovedBy(entityManager.getReference(com.sep490.backendclubmanagement.entity.User.class, approverId));
        p.setApprovedAt(java.time.LocalDateTime.now());

        // Clear các dấu vết reject cũ (nếu có)
        p.setRejectedBy(null);
        p.setRejectedAt(null);

        postRepository.save(p);

        // 🔔 Gửi notification cho tác giả bài post
        try {
            if (p.getCreatedBy() != null && !p.getCreatedBy().getId().equals(approverId)) {
                // Không gửi notification nếu tự approve bài của mình
                String title = "Bài viết của bạn đã được duyệt";
                String message = p.getTitle() != null && !p.getTitle().isEmpty()
                        ? "Bài viết: \"" + p.getTitle() + "\""
                        : "Bài viết của bạn";
                String actionUrl = "/posts/" + postId;

                notificationService.sendToUser(
                        p.getCreatedBy().getId(),
                        approverId, // người duyệt
                        title,
                        message,
                        NotificationType.POST_APPROVED,
                        NotificationPriority.NORMAL,
                        actionUrl,
                        p.getClub() != null ? p.getClub().getId() : null,
                        null, // relatedNewsId
                        p.getTeam() != null ? p.getTeam().getId() : null, // relatedTeamId
                        null, // relatedRequestId
                        null  // relatedEventId
                );

                log.info("[Post] Notification sent to user {}: post approved {}", p.getCreatedBy().getId(), postId);
            }
        } catch (Exception e) {
            log.error("[Post] Failed to send approval notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break post approval
        }

        return ApiResponse.success(null);
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RejectRequest {        // 👈 DTO nhỏ gọn cho body từ chối
        private String reason;
    }
    @PostMapping("/{postId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long postId, @RequestBody(required = false) RejectRequest body) throws AppException {
        Long approverId = userService.getCurrentUserId();
        Post p = postRepository.findById(postId).orElseThrow();

        if (!postService.canApprove(approverId, p)) {
            throw new IllegalArgumentException("Bạn không có quyền từ chối bài này");
        }

        p.setStatus(PostStatus.REJECTED);
        p.setRejectedBy(entityManager.getReference(com.sep490.backendclubmanagement.entity.User.class, approverId));
        p.setRejectedAt(java.time.LocalDateTime.now());

        // Nếu bạn đã thêm cột rejectReason:
        String reason = (body != null) ? body.getReason() : null;
        try {
            p.getClass().getDeclaredField("rejectReason");
            p.setRejectReason((reason != null && !reason.isBlank()) ? reason : null);
        } catch (NoSuchFieldException ignored) {}

        // Nếu đã bị từ chối, clear dấu vết "approved" cũ (tránh thông tin mâu thuẫn)
        p.setApprovedBy(null);
        p.setApprovedAt(null);

        postRepository.save(p);

        // 🔔 Gửi notification cho tác giả bài post
        try {
            if (p.getCreatedBy() != null && !p.getCreatedBy().getId().equals(approverId)) {
                // Không gửi notification nếu tự reject bài của mình
                String title = "Bài viết của bạn đã bị từ chối";

                // Build message với lý do từ chối (nếu có)
                StringBuilder messageBuilder = new StringBuilder();
                if (p.getTitle() != null && !p.getTitle().isEmpty()) {
                    messageBuilder.append("Bài viết: \"").append(p.getTitle()).append("\"");
                } else {
                    messageBuilder.append("Bài viết của bạn");
                }

                if (reason != null && !reason.isBlank()) {
                    messageBuilder.append(" - Lý do: ").append(reason);
                }

                String message = messageBuilder.toString();
                String actionUrl = "/posts/" + postId;

                notificationService.sendToUser(
                        p.getCreatedBy().getId(),
                        approverId, // người từ chối
                        title,
                        message,
                        NotificationType.POST_REJECTED,
                        NotificationPriority.HIGH, // Priority cao vì cần biết lý do để sửa
                        actionUrl,
                        p.getClub() != null ? p.getClub().getId() : null,
                        null, // relatedNewsId
                        p.getTeam() != null ? p.getTeam().getId() : null, // relatedTeamId
                        null, // relatedRequestId
                        null  // relatedEventId
                );

                log.info("[Post] Notification sent to user {}: post rejected {} with reason: {}",
                        p.getCreatedBy().getId(), postId, reason);
            }
        } catch (Exception e) {
            log.error("[Post] Failed to send rejection notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break post rejection
        }

        return ApiResponse.success(null);
    }
    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String prop = parts.length > 0 ? parts[0] : "createdAt";
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }
    // Search bài viết trong 1 CLB theo role (chủ nhiệm/phó vs member)
    @GetMapping("/{clubId}/search")
    public ApiResponse<Page<PostWithRelationsData>> searchInClub(
            @PathVariable Long clubId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    )throws Exception {
        Long userId = userService.getCurrentUserId(); // giống getClubFeed

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PostWithRelationsData> data = postService.searchPostsInClub(
                clubId,
                userId,
                keyword,
                pageable
        );
        return ApiResponse.success(data);
    }
}

