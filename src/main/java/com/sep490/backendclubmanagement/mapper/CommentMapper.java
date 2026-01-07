package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.CommentDTO;
import com.sep490.backendclubmanagement.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
//import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "parentComment.id", target = "parentId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userName")
    @Mapping(source = "user.avatarUrl", target = "userAvatar")
    @Mapping(source = "isEdited", target = "edited")
    @Mapping(source = "rootParentCommentId", target = "rootParentId")
    @Mapping(target = "replies", ignore = true)
    CommentDTO toDTO(Comment comment);

    List<CommentDTO> toDTOs(List<Comment> comments);
}
