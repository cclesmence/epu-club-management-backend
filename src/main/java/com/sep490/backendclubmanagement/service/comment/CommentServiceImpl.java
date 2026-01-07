package com.sep490.backendclubmanagement.service.comment;

import com.sep490.backendclubmanagement.dto.response.CommentDTO;
import com.sep490.backendclubmanagement.dto.websocket.CommentWebSocketPayload;
import com.sep490.backendclubmanagement.entity.Comment;
import com.sep490.backendclubmanagement.entity.NotificationPriority;
import com.sep490.backendclubmanagement.entity.NotificationType;
import com.sep490.backendclubmanagement.entity.Post;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.mapper.CommentMapper;
import com.sep490.backendclubmanagement.repository.CommentRepository;
import com.sep490.backendclubmanagement.repository.PostRepository;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepo;
    private final PostRepository postRepo;
    private final UserService userService;      // đã có sẵn trong project bạn
    private final CommentMapper commentMapper;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    // 👈 Inject mapper mới tách

    /* ====== CREATE ====== */
    @Override
    @Transactional
    public CommentDTO create(Long postId, Long userId, String content, Long parentId){
        if (content == null || content.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty");

        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Comment parent = null;
        Long rootParentId = null;


        if (parentId != null) {
             parent = commentRepo.findActiveById(parentId);
            // parent phải tồn tại và cùng post
            if (parent == null || !parent.getPost().getId().equals(postId))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parent comment");

            rootParentId = (parent.getRootParentCommentId() != null)
                    ? parent.getRootParentCommentId()
                    : parent.getId();



            // ✅ Chuẩn hoá về 2 cấp:
            // nếu parent là reply (có parentComment != null) thì gắn về cha top-level của nó
//            Comment topLevel = (parent.getParentComment() == null)
//                    ? parent
//                    : parent.getParentComment();

            // (phòng hờ) đảm bảo vẫn cùng post
            if (!parent.getPost().getId().equals(postId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parent comment");
            }


        }
        Comment c = Comment.builder()
                .post(post)
                .user(user)
                .content(content.trim())
                .isEdited(false)
                .parentComment(parent)          // 👈 set parent luôn ở đây
                .rootParentCommentId(rootParentId) // 👈 NEW: set root cha cấp 1 cho reply
                .build();


        Comment saved = commentRepo.save(c);
        CommentDTO commentDTO = commentMapper.toDTO(saved);
        
        // Gửi WebSocket notification
        try {
            CommentWebSocketPayload payload = CommentWebSocketPayload.builder()
                    .comment(commentDTO)
                    .postId(postId)
                    .action("NEW")
                    .build();
            
            // Broadcast đến club nếu post có club
            if (post.getClub() != null) {
                webSocketService.broadcastToClub(
                        post.getClub().getId(),
                        "POST",
                        "COMMENT_NEW",
                        payload
                );
            }
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến việc tạo comment
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
        
        // ✅ Gửi notification sau khi tạo comment thành công
        try {
            if (parent != null) {
                // Trường hợp REPLY comment: gửi notification cho người được reply
                User parentAuthor = parent.getUser();
                if (parentAuthor != null && !parentAuthor.getId().equals(userId)) {
                    // Không gửi notification cho chính mình
                    String title = user.getFullName() + " đã trả lời bình luận của bạn";
                    String message = "\"" + content.trim() + "\"";
                    String actionUrl = "/posts/" + postId + "/comments/" + saved.getId();

                    notificationService.sendToUser(
                            parentAuthor.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.POST_REPLIED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            post.getClub() != null ? post.getClub().getId() : null,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null, // relatedRequestId
                            null  // relatedEventId
                    );
                }

                // ✅ BONUS: Cũng gửi cho tác giả bài post (nếu khác người được reply và khác người comment)
                User postAuthor = post.getCreatedBy();
                if (postAuthor != null && 
                    !postAuthor.getId().equals(userId) && 
                    parentAuthor != null &&
                    !postAuthor.getId().equals(parentAuthor.getId())) {

                    String title = user.getFullName() + " đã bình luận trong bài viết của bạn";
                    String message = "\"" + content.trim() + "\"";
                    String actionUrl = "/posts/" + postId + "/comments/" + saved.getId();

                    notificationService.sendToUser(
                            postAuthor.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.POST_COMMENTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            post.getClub() != null ? post.getClub().getId() : null,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null, // relatedRequestId
                            null  // relatedEventId
                    );
                }
            } else {
                // Trường hợp COMMENT mới (không phải reply): gửi notification cho tác giả bài post
                User postAuthor = post.getCreatedBy();
                if (postAuthor != null && !postAuthor.getId().equals(userId)) {
                    // Không gửi notification cho chính mình
                    String title = user.getFullName() + " đã bình luận trong bài viết của bạn";
                    String message = "\"" + content.trim() + "\"";
                    String actionUrl = "/posts/" + postId + "/comments/" + saved.getId();

                    notificationService.sendToUser(
                            postAuthor.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.POST_COMMENTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            post.getClub() != null ? post.getClub().getId() : null,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null, // relatedRequestId
                            null  // relatedEventId
                    );
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến việc tạo comment
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return commentDTO; // 👈 dùng mapper
    }

    /* ====== LIST TOP-LEVEL (không kèm replies) ====== */
    @Override
    public List<CommentDTO> listTopLevel(Long postId, int page, int size){
        var list = commentRepo.findTopLevelByPost(postId, PageRequest.of(page, size));
        return commentMapper.toDTOs(list); // 👈 dùng mapper
    }

    /* ====== LIST REPLIES (không kèm replies của replies) ====== */
    @Override
    public List<CommentDTO> listReplies(Long parentId){
        var list = commentRepo.findReplies(parentId);
        return commentMapper.toDTOs(list); // 👈 dùng mapper
    }

    /* ====== EDIT ====== */
    @Override
    @Transactional
    public CommentDTO edit(Long commentId, Long editorUserId, String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty");
        }

        Comment c = commentRepo.findActiveById(commentId);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        // ✅ Chỉ cho sửa nếu đúng chủ comment
        if (!Objects.equals(c.getUser().getId(), editorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot edit others' comments");
        }

        c.setContent(newContent.trim());
        c.setIsEdited(true);
        Comment saved = commentRepo.save(c);
        CommentDTO commentDTO = commentMapper.toDTO(saved);
        
        // Gửi WebSocket notification cho edit
        try {
            CommentWebSocketPayload payload = CommentWebSocketPayload.builder()
                    .comment(commentDTO)
                    .postId(c.getPost().getId())
                    .action("EDIT")
                    .build();
            
            if (c.getPost().getClub() != null) {
                webSocketService.broadcastToClub(
                        c.getPost().getClub().getId(),
                        "POST",
                        "COMMENT_EDIT",
                        payload
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
        
        return commentDTO;
    }

    /* ====== SOFT DELETE ====== */
    @Override
    @Transactional
    public void softDelete(Long commentId, Long requesterId) {
        Comment c = commentRepo.findActiveById(commentId);
        if (c == null) {
            // tuỳ bạn: có thể throw 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        Long ownerId = c.getUser().getId();
        Long postAuthorId = c.getPost().getCreatedBy().getId(); // field createdBy đã có trong Post

        // Chỉ chủ comment hoặc chủ bài post được xoá
        if (!Objects.equals(ownerId, requesterId) &&
                !Objects.equals(postAuthorId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this comment");
        }

        // Gom toàn bộ id: chính nó + mọi hậu duệ (BFS)
        List<Long> toDelete = collectDescendantIdsBfs(c.getId());
        toDelete.add(0, c.getId()); // include parent first

        // Soft delete hàng loạt
        commentRepo.bulkSoftDeleteByIds(toDelete, LocalDateTime.now());
        
        // Gửi WebSocket notification cho delete
        try {
            CommentDTO deletedDTO = commentMapper.toDTO(c);
            CommentWebSocketPayload payload = CommentWebSocketPayload.builder()
                    .comment(deletedDTO)
                    .postId(c.getPost().getId())
                    .action("DELETE")
                    .build();
            
            if (c.getPost().getClub() != null) {
                webSocketService.broadcastToClub(
                        c.getPost().getClub().getId(),
                        "POST",
                        "COMMENT_DELETE",
                        payload
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
    }

    /** Duyệt BFS để gom toàn bộ id con/cháu... (tránh đệ quy sâu) */
    private List<Long> collectDescendantIdsBfs(Long rootId) {
        List<Long> result = new ArrayList<>();
        Deque<Long> q = new ArrayDeque<>();
        q.add(rootId);

        while (!q.isEmpty()) {
            Long cur = q.poll();
            List<Long> children = commentRepo.findActiveChildIds(cur);
            if (!children.isEmpty()) {
                result.addAll(children);
                children.forEach(q::add);
            }
        }
        return result; // KHÔNG gồm root
    }

    /* ====== (TUỲ CHỌN) Build cây thread đệ quy khi cần ======
       Nếu muốn trả toàn bộ cây: gọi mapWithReplies(root) thay vì toDTO(root)
       Không để vào mapper để tránh mapper phụ thuộc repository.
    */
    private CommentDTO mapWithReplies(Comment c){
        CommentDTO dto = commentMapper.toDTO(c);
        var children = commentRepo.findReplies(c.getId());
        dto.setReplies(children.stream().map(this::mapWithReplies).toList());
        return dto;
    }
    @Override
    public List<CommentDTO> getAllFlat(Long postId) {
        return commentRepo.findAllActiveByPost(postId)
                .stream().map(commentMapper::toDTO).toList();
    }
}
