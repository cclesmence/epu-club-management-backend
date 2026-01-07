// src/main/java/com/sep490/backendclubmanagement/service/ICommentService.java
package com.sep490.backendclubmanagement.service.comment;

import com.sep490.backendclubmanagement.dto.response.CommentDTO;

import java.util.List;

public interface ICommentService {
    CommentDTO create(Long postId, Long userId, String content, Long parentId);
    List<CommentDTO> listTopLevel(Long postId, int page, int size);
    List<CommentDTO> listReplies(Long parentId);
    CommentDTO edit(Long commentId, Long editorId, String newContent);
    void softDelete(Long commentId, Long requesterId);
    // ICommentService.java
    List<CommentDTO> getAllFlat(Long postId);



}
