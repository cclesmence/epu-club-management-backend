package com.sep490.backendclubmanagement.service.post;

import com.sep490.backendclubmanagement.dto.request.CreatePostRequest;
import com.sep490.backendclubmanagement.dto.request.UpdatePostRequest;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.PostRepository;
import com.sep490.backendclubmanagement.service.club.role.ClubRoleService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.util.PostStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final ClubRoleService clubRoleService;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final NotificationService notificationService ;

    @PersistenceContext
    private EntityManager em;


    // Bài toàn CLB
    public Page<PostWithRelationsData> getClubWidePosts(Long clubId, Pageable pageable) {
        Page<Post> page = postRepository.findClubWidePosts(clubId, true, "PUBLISHED", pageable);
        return page.map(this::toDetailsDTO);
    }

    // Bài theo team của CLB
    public Page<PostWithRelationsData> getTeamPosts(Long clubId, Long teamId, Pageable pageable) {
        Page<Post> page = postRepository.findTeamPosts(clubId, teamId, "PUBLISHED", pageable);
        return page.map(this::toDetailsDTO);
    }

    public Page<PostWithRelationsData> getClubFeed(Long clubId, Long userId, Pageable pageable) {
        // 1) Kiểm tra có phải Chủ nhiệm / Phó chủ nhiệm CLB không
        boolean isClubBoss = clubRoleService.isClubLeaderOrVice(userId, clubId);

        if (isClubBoss) {
            // Chủ nhiệm / phó: thấy toàn bộ post trong CLB
            Page<Post> page = postRepository.findByClub_IdAndStatus(
                    clubId,
                    PostStatus.PUBLISHED,
                    pageable
            );
            return page.map(this::toDetailsDTO);
        } else {
            // Member / trưởng ban: thấy club-wide + các team mình thuộc (kỳ hiện tại, active)
            var teamIds = clubMemberShipRepository.findTeamIdsByUserAndClubAndStatus(
                    userId,
                    clubId,
                    ClubMemberShipStatus.ACTIVE
            );

            if (teamIds == null || teamIds.isEmpty()) {
                // Không thuộc team nào → chỉ thấy bài toàn CLB
                Page<Post> page = postRepository.findClubWidePosts(
                        clubId,
                        true,                       // clubWide
                        PostStatus.PUBLISHED,
                        pageable
                );
                return page.map(this::toDetailsDTO);
            }

            Page<Post> page = postRepository.findFeedForMemberInClub(
                    clubId,
                    PostStatus.PUBLISHED,
                    teamIds,
                    pageable
            );
            return page.map(this::toDetailsDTO);
        }
    }


    // Bài chờ duyệt toàn CLB (club-wide pending)
    public Page<PostWithRelationsData> getPendingClubWidePosts(Long clubId, Pageable pageable) {
        Page<Post> page = postRepository.findPendingClubWidePosts(clubId, PostStatus.PENDING, pageable);
        return page.map(this::toDetailsDTO);
    }

    // Bài chờ duyệt theo team
    public Page<PostWithRelationsData> getPendingTeamPosts(Long clubId, Long teamId, Pageable pageable) {
        Page<Post> page = postRepository.findPendingTeamPosts(clubId, teamId, PostStatus.PENDING, pageable);
        return page.map(this::toDetailsDTO);
    }

    //search
    public Page<PostWithRelationsData> searchPostsInClub(
            Long clubId,
            Long userId,
            String keyword,
            Pageable pageable
    ) {
        // 0) Chuẩn hoá keyword
        String q = (keyword == null) ? "" : keyword.trim();
        if (q.isEmpty()) {
            return Page.empty(pageable);
        }

        // 1) Check quyền admin CLB (Chủ nhiệm / Phó)
        boolean isClubBoss = clubRoleService.isClubLeaderOrVice(userId, clubId);

        List<Post> collected = new ArrayList<>();

        if (isClubBoss) {
            // === ADMIN: thấy TẤT CẢ bài trong CLB ===
            collected.addAll(
                    postRepository.searchAdminScope(
                            clubId,
                            PostStatus.PUBLISHED,
                            q
                    )
            );
        } else {
            // === MEMBER / TEAM LEAD ===

            // (a) Bài club-wide trong CLB
            collected.addAll(
                    postRepository.searchClubWideOnly(
                            clubId,
                            PostStatus.PUBLISHED,
                            q
                    )
            );

            // (b) Bài của các team mà user đang tham gia trong CLB
            List<Long> teamIds = clubMemberShipRepository.findTeamIdsByUserAndClubAndStatus(
                    userId,
                    clubId,
                    ClubMemberShipStatus.ACTIVE
            );

            if (teamIds != null && !teamIds.isEmpty()) {
                collected.addAll(
                        postRepository.searchTeamScope(
                                clubId,
                                teamIds,
                                PostStatus.PUBLISHED,
                                q
                        )
                );
            }
        }

        // 2) Loại trùng + sort theo createdAt desc
        List<Post> distinct = collected.stream()
                .distinct()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .toList();

        // 3) Phân trang thủ công
        int start = (int) pageable.getOffset();
        if (start >= distinct.size()) {
            return new PageImpl<>(List.of(), pageable, distinct.size());
        }
        int end = Math.min(start + pageable.getPageSize(), distinct.size());

        List<PostWithRelationsData> dtoPage = distinct.subList(start, end).stream()
                .map(this::toDetailsDTO)
                .toList();

        return new PageImpl<>(dtoPage, pageable, distinct.size());
    }

    @Transactional
    public PostWithRelationsData createPostWithUploads(
            CreatePostRequest req,
            List<MultipartFile> files,
            Long authorId
    ) {
        // 1) Validate cờ clubWide/teamId
        if (Boolean.TRUE.equals(req.getClubWide())) {
            req.setTeamId(null);
        } else if (req.getTeamId() == null) {
            throw new IllegalArgumentException("teamId is required when clubWide = false");
        }

        // CHANGE: status do server tự quyết, KHÔNG lấy từ req
        String status;
        Long clubId = req.getClubId();
        Long teamId = req.getTeamId();
        boolean isClubWide = Boolean.TRUE.equals(req.getClubWide()) && teamId == null;

        boolean isClubPresident = clubRoleService.isClubLeaderOrVice(authorId, clubId);
        boolean isTeamLead = (teamId != null) && clubRoleService.isTeamLeader(authorId, teamId);

        if (isClubPresident){
            status = PostStatus.PUBLISHED;          // Chủ nhiệm/Phó đăng ⇒ auto publish
        } else if (!isClubWide && isTeamLead) {
            status = PostStatus.PUBLISHED;          // Trưởng ban đăng đúng team ⇒ auto publish
        } else {
            status = PostStatus.PENDING;            // Còn lại ⇒ chờ duyệt
        }


        // 2) Tham chiếu
        Club clubRef = em.getReference(Club.class, req.getClubId());
        Team teamRef = (req.getTeamId() != null) ? em.getReference(Team.class, req.getTeamId()) : null;
        User authorRef = (authorId != null) ? em.getReference(User.class, authorId) : null;

        // 3) Tạo Post
        Post p = new Post();
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setStatus(status);
        if (PostStatus.PUBLISHED.equals(status)) {
            p.setApprovedBy(authorRef);
            p.setApprovedAt(LocalDateTime.now());
            p.setRejectedBy(null);
            p.setRejectedAt(null);
            p.setRejectReason(null);
        }
        p.setIsClubWide(Boolean.TRUE.equals(req.getClubWide()));
        if (req.getWithinClub() != null) p.setIsWithinClub(req.getWithinClub());
        p.setCreatedAt(LocalDateTime.now());
        p.setClub(clubRef);
        p.setTeam(teamRef);
        p.setCreatedBy(authorRef);

        // 4) Chuẩn hóa media từ 2 nguồn: (A) metadata trong req.media, (B) files upload
        List<CreatePostRequest.PostMediaItem> meta = (req.getMedia() == null)
                ? new ArrayList<>()
                : new ArrayList<>(req.getMedia());

        Set<PostMedia> mediaSet = new LinkedHashSet<>();
        // --- A) Nếu có files => upload lên Cloudinary (song song bằng @Async) ---
        if (files != null && !files.isEmpty()) {
            // Tạo danh sách futures tương ứng với từng file để giữ được thứ tự
            List<CompletableFuture<PostMedia>> futures = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                final int idx = i;
                MultipartFile f = files.get(i);

                // Lấy metadata tương ứng nếu có
                CreatePostRequest.PostMediaItem mm = (idx < meta.size()) ? meta.get(idx) : null;

                // Gọi upload bất đồng bộ và map sang PostMedia
                CompletableFuture<PostMedia> fu = cloudinaryService.uploadImageAsync(f)
                        .thenApply(up -> {
                            PostMedia pm = new PostMedia();
                            pm.setTitle(mm != null && mm.getTitle() != null
                                    ? mm.getTitle()
                                    : filenameNoExt(f.getOriginalFilename()));
                            pm.setMediaUrl(up.url());     // URL ảnh Cloudinary
                            pm.setMediaType("IMAGE");
                            pm.setCaption(mm != null ? mm.getCaption() : null);
                            pm.setDisplayOrder(mm != null ? mm.getDisplayOrder() : idx); // theo thứ tự file
                            pm.setCreatedAt(LocalDateTime.now());
                            pm.setPost(p);
                            return pm;
                        });

                futures.add(fu);
            }

            // Chờ tất cả upload hoàn tất trước khi lưu DB để đảm bảo nhất quán
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Gom kết quả (nếu muốn skip file lỗi, bọc try/catch ở đây)
            for (CompletableFuture<PostMedia> fu : futures) {
                mediaSet.add(fu.join());
            }
        }

        if (meta.size() > (files == null ? 0 : files.size())) {
            for (int i = (files == null ? 0 : files.size()); i < meta.size(); i++) {
                CreatePostRequest.PostMediaItem mm = meta.get(i);
                if (mm.getMediaUrl() == null || mm.getMediaUrl().isBlank()) continue; // bỏ nếu thiếu URL

                PostMedia pm = new PostMedia();
                pm.setTitle(mm.getTitle());
                pm.setMediaUrl(mm.getMediaUrl());
                pm.setMediaType(mm.getMediaType() != null ? mm.getMediaType() : "IMAGE");
                pm.setCaption(mm.getCaption());
                pm.setDisplayOrder(mm.getDisplayOrder() != null ? mm.getDisplayOrder() : i);
                pm.setCreatedAt(LocalDateTime.now());
                pm.setPost(p);
                mediaSet.add(pm);
            }
        }

        p.setPostMedia(mediaSet);

        // 5) Lưu
        Post saved = postRepository.save(p);

        // 🔔 Gửi notification cho club managers nếu post PENDING (cần duyệt)
        if (PostStatus.PENDING.equals(status)) {
            try {
                // Lấy danh sách managers (Chủ nhiệm/Phó chủ nhiệm) của club
                List<Long> managerIds = notificationService.getClubManagers(clubId);

                if (!managerIds.isEmpty() && authorId != null) {
                    String title = "Bài viết mới cần duyệt";
                    String message = saved.getTitle() != null && !saved.getTitle().isEmpty()
                            ? "Bài viết: \"" + saved.getTitle() + "\""
                            : "Có bài viết mới cần duyệt";
                    String actionUrl = "/posts/" + saved.getId();

                    notificationService.sendToUsers(
                            managerIds,
                            authorId, // tác giả
                            title,
                            message,
                            NotificationType.POST_PENDING_APPROVAL,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            clubId,
                            null, // relatedNewsId
                            teamId, // relatedTeamId
                            null  // relatedRequestId
                    );

                    System.out.println("[Post] Notification sent to " + managerIds.size() + " managers: post pending approval " + saved.getId());
                }
            } catch (Exception e) {
                System.err.println("[Post] Failed to send pending approval notification: " + e.getMessage());
                // Don't throw - notification failure shouldn't break post creation
            }
        }

        return toDetailsDTO(saved);
    }

    private String filenameNoExt(String name) {
        if (name == null) return "image";
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }

    @Transactional
    public PostWithRelationsData updatePostWithUploads(
            Long postId,
            UpdatePostRequest req,
            List<MultipartFile> files,
            Long authorId
    ) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        // --- 1) Club/Team theo clubWide ---
        if (req.getClubId() != null && (p.getClub() == null || !p.getClub().getId().equals(req.getClubId()))) {
            p.setClub(em.getReference(Club.class, req.getClubId()));
        }
        if (req.getClubWide() != null) {
            boolean cw = req.getClubWide();
            p.setIsClubWide(cw);
            if (cw) {
                p.setTeam(null);
            } else {
                if (req.getTeamId() == null) {
                    throw new IllegalArgumentException("teamId is required when clubWide = false");
                }
                p.setTeam(em.getReference(Team.class, req.getTeamId()));
            }
        } else if (req.getTeamId() != null) {
            p.setTeam(em.getReference(Team.class, req.getTeamId()));
        }

        // --- 2) Trường đơn ---
        if (req.getWithinClub() != null) p.setIsWithinClub(req.getWithinClub());
        if (req.getTitle() != null)      p.setTitle(req.getTitle());
        if (req.getContent() != null)    p.setContent(req.getContent());
        // NEW: nếu đang chờ duyệt, người cập nhật đủ quyền thì auto publish
        if (PostStatus.PENDING.equals(p.getStatus())) {
            boolean isClubWide = p.isIsClubWide();
            if (isClubWide && clubRoleService.isClubLeaderOrVice(authorId, p.getClub().getId())) {
                p.setStatus(PostStatus.PUBLISHED);
            } else if (!isClubWide && p.getTeam() != null
                    && clubRoleService.isTeamLeader(authorId, p.getTeam().getId())) {
                p.setStatus(PostStatus.PUBLISHED);
            }
        }

        // --- 3) Xóa media cũ theo ID (DB-only) ---
        if (req.getDeleteMediaIds() != null && !req.getDeleteMediaIds().isEmpty() && p.getPostMedia() != null) {
            Iterator<PostMedia> it = p.getPostMedia().iterator();
            while (it.hasNext()) {
                PostMedia m = it.next();
                if (req.getDeleteMediaIds().contains(m.getId())) {
                    it.remove();     // gỡ khỏi tập con của Post
                    em.remove(m);     // xóa bản ghi PostMedia trong DB
                }
            }
        }

        // --- 4) Thêm media mới từ files (upload đã có sẵn trong create của bạn) ---
        List<UpdatePostRequest.NewMediaMeta> metas =
                (req.getNewMediasMeta() == null) ? List.of() : req.getNewMediasMeta();

        if (files != null && !files.isEmpty()) {
            if (p.getPostMedia() == null) p.setPostMedia(new LinkedHashSet<>());

            final int baseOrder = calcNextOrder(p);            // 👉 THÊM: chốt order bắt đầu

            List<CompletableFuture<PostMedia>> futures = new ArrayList<>();  // 👉 THÊM: list futures

            for (int i = 0; i < files.size(); i++) {
                final int idx = i;
                MultipartFile f = files.get(i);
                UpdatePostRequest.NewMediaMeta meta = (idx < metas.size()) ? metas.get(idx) : null;

                // 👉 ĐỔI: dùng upload async thay vì đồng bộ
                CompletableFuture<PostMedia> fu = cloudinaryService.uploadImageAsync(f)
                        .thenApply(up -> {
                            PostMedia pm = new PostMedia();
                            pm.setTitle(meta != null && meta.getTitle() != null
                                    ? meta.getTitle()
                                    : filenameNoExt(f.getOriginalFilename()));
                            pm.setMediaUrl(up.url()); // chỉ lưu URL
                            pm.setMediaType(meta != null && meta.getMediaType() != null ? meta.getMediaType() : "IMAGE");
                            pm.setCaption(meta != null ? meta.getCaption() : null);

                            // 👉 THÊM: tránh trùng order khi chạy song song
                            Integer order = (meta != null && meta.getDisplayOrder() != null)
                                    ? meta.getDisplayOrder()
                                    : (baseOrder + idx);
                            pm.setDisplayOrder(order);

                            pm.setCreatedAt(LocalDateTime.now());
                            pm.setPost(p);
                            return pm;
                        });

                futures.add(fu); // 👉 THÊM
            }

            // 👉 THÊM: đợi TẤT CẢ upload xong mới thêm vào post và lưu DB
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<PostMedia> fu : futures) {
                p.getPostMedia().add(fu.join()); // (có thể bọc try/catch nếu muốn skip lỗi từng file)
            }
        }


        Post saved = postRepository.save(p);
        return toDetailsDTO(saved);
    }

    private int calcNextOrder(Post p) {
        if (p.getPostMedia() == null || p.getPostMedia().isEmpty()) return 0;
        return p.getPostMedia().stream()
                .map(pm -> pm.getDisplayOrder() == null ? 0 : pm.getDisplayOrder())
                .max(Integer::compareTo).orElse(0) + 1;
    }
    @Transactional
    public void deletePost(Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        // DB-only: KHÔNG gọi xóa Cloudinary; chỉ xóa dữ liệu trong DB
        postRepository.delete(p); // cascade ALL trên postMedia sẽ xóa media con
    }

    @Transactional
    public PostWithRelationsData deleteOneMedia(Long postId, Long mediaId) {
        // 1) Tải Post + tập media (để kiểm tra quan hệ)
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        if (p.getPostMedia() == null || p.getPostMedia().isEmpty()) {
            throw new IllegalArgumentException("Post has no media to delete");
        }

        // 2) Tìm media theo id và đảm bảo nó thuộc về post này
        PostMedia target = null;
        for (PostMedia m : p.getPostMedia()) {
            if (Objects.equals(m.getId(), mediaId)) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Media " + mediaId + " does not belong to post " + postId);
        }

        // 3) Gỡ khỏi tập con và xóa DB (DB-only; không gọi Cloudinary)
        p.getPostMedia().remove(target);
        em.remove(target);

        // (Tuỳ chọn) 4) Re-order displayOrder cho gọn (0..N-1 theo thứ tự hiện tại)
        reindexDisplayOrder(p);

        // 5) Lưu và trả DTO
        Post saved = postRepository.save(p);
        return toDetailsDTO(saved);
    }



    /** Tuỳ chọn: sắp xếp lại displayOrder tăng dần, null sẽ bị đẩy về cuối và đánh lại số. */
    private void reindexDisplayOrder(Post p) {
        if (p.getPostMedia() == null || p.getPostMedia().isEmpty()) return;

        List<PostMedia> sorted = p.getPostMedia().stream()
                .sorted(Comparator.comparing(pm -> pm.getDisplayOrder() == null ? Integer.MAX_VALUE : pm.getDisplayOrder()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setDisplayOrder(i);
        }
    }


    private PostWithRelationsData toDetailsDTO(Post p) {
        // sort media theo displayOrder (null -> cuối)
        List<PostMediaData> medias = p.getPostMedia() == null ? List.of()
                : p.getPostMedia().stream()
                .sorted(Comparator.comparing(pm -> pm.getDisplayOrder() == null ?
                        Integer.MAX_VALUE : pm.getDisplayOrder()))
                .map(pm -> PostMediaData.builder()
                        .id(pm.getId())
                        .title(pm.getTitle())
                        .mediaUrl(pm.getMediaUrl())
                        .mediaType(pm.getMediaType())
                        .caption(pm.getCaption())
                        .displayOrder(pm.getDisplayOrder())
                        .createdAt(pm.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<CommentData> comments = p.getComments() == null ? List.of()
                : p.getComments().stream()
                .map(c -> CommentData.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .isEdited(Boolean.TRUE.equals(c.getIsEdited()))
                        .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                        .userId(c.getUser() != null ? c.getUser().getId() : null)
                        .userName(c.getUser() != null ? c.getUser().getFullName() : null)
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<LikeData> likes = p.getLikes() == null ? List.of()
                : p.getLikes().stream()
                .map(l -> LikeData.builder()
                        .id(l.getId())
                        .userId(l.getUser() != null ? l.getUser().getId() : null)
                        .userName(l.getUser() != null ? l.getUser().getFullName() : null)
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PostWithRelationsData.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .status(p.getStatus())// nếu bạn muốn hiển thị cờ phụ
                .clubWide(p.isIsClubWide())
                .createdAt(p.getCreatedAt())
                .approvedById(p.getApprovedBy() != null ? p.getApprovedBy().getId() : null)
                .approvedByName(p.getApprovedBy() != null ? p.getApprovedBy().getFullName() : null)
                .approvedAt(p.getApprovedAt())
                .rejectedById(p.getRejectedBy() != null ? p.getRejectedBy().getId() : null)
                .rejectedByName(p.getRejectedBy() != null ? p.getRejectedBy().getFullName() : null)
                .rejectedAt(p.getRejectedAt())
                .rejectReason(p.getRejectReason())
                .teamId(p.getTeam() != null ? p.getTeam().getId() : null)
                .teamName(p.getTeam() != null ? p.getTeam().getTeamName() : null)
                .clubId(p.getClub() != null ? p.getClub().getId() : null)
                .clubName(p.getClub() != null ? p.getClub().getClubName() : null)
                .authorId(p.getCreatedBy() != null ? p.getCreatedBy().getId() : null)
                .authorName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null)
                .authorAvatarUrl(p.getCreatedBy() != null ? p.getCreatedBy().getAvatarUrl() : null)
                .media(medias)
                .comments(comments)
                .likes(likes)
                .build();
    }


    public boolean canApprove(Long userId, Post p){
        if (Boolean.TRUE.equals(p.isIsClubWide())) {
            return clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId());
        } else {
            boolean teamLead = p.getTeam()!=null && clubRoleService.isTeamLeader(userId, p.getTeam().getId());
            boolean clubBoss = clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId()); // cho phép override
            return teamLead || clubBoss;
        }
    }

}