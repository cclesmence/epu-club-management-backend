package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreatePostRequest;
import com.sep490.backendclubmanagement.dto.request.UpdatePostRequest;
import com.sep490.backendclubmanagement.dto.response.PostWithRelationsData;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.PostRepository;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.club.role.ClubRoleService;
import com.sep490.backendclubmanagement.service.post.PostService;
import com.sep490.backendclubmanagement.util.PostStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho PostService dùng JUnit5 + Mockito
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ClubRoleService clubRoleService;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private EntityManager em;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void injectEntityManager() {
        // vì EntityManager em trong PostService được inject bằng @PersistenceContext,
        // nên trong unit test phải tự set bằng ReflectionTestUtils
        ReflectionTestUtils.setField(postService, "em", em);
    }

    // ========= Helper ============

    private Post buildPost(Long id, Long clubId, Long teamId, boolean clubWide, String status) {
        Club c = new Club();
        c.setId(clubId);

        Team t = null;
        if (teamId != null) {
            t = new Team();
            t.setId(teamId);
        }

        User u = new User();
        u.setId(999L);
        u.setFullName("Author A");

        Post p = new Post();
        p.setId(id);
        p.setClub(c);
        p.setTeam(t);
        p.setCreatedBy(u);
        p.setTitle("Title " + id);
        p.setContent("Content " + id);
        p.setIsClubWide(clubWide);
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now().minusMinutes(id));
        return p;
    }

    // ==========================================
    // getClubWidePosts, getTeamPosts, getClubFeed
    // ==========================================

    @Test
    void getClubWidePosts_shouldReturnMappedPage() {
        Long clubId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Post p = buildPost(100L, clubId, null, true, PostStatus.PUBLISHED);
        Page<Post> page = new PageImpl<>(List.of(p), pageable, 1);

        when(postRepository.findClubWidePosts(clubId, true, "PUBLISHED", pageable))
                .thenReturn(page);

        Page<PostWithRelationsData> result = postService.getClubWidePosts(clubId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
        assertEquals("Title 100", result.getContent().get(0).getTitle());
    }

    @Test
    void getTeamPosts_shouldReturnMappedPage() {
        Long clubId = 1L;
        Long teamId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        Post p = buildPost(101L, clubId, teamId, false, PostStatus.PUBLISHED);
        Page<Post> page = new PageImpl<>(List.of(p), pageable, 1);

        when(postRepository.findTeamPosts(clubId, teamId, "PUBLISHED", pageable))
                .thenReturn(page);

        Page<PostWithRelationsData> result = postService.getTeamPosts(clubId, teamId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(teamId, result.getContent().get(0).getTeamId());
    }

    @Test
    void getClubFeed_whenUserIsClubBoss_shouldSeeAllPostsInClub() {
        Long clubId = 1L;
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        Post p1 = buildPost(1L, clubId, null, true, PostStatus.PUBLISHED);
        Page<Post> page = new PageImpl<>(List.of(p1), pageable, 1);

        when(clubRoleService.isClubLeaderOrVice(userId, clubId)).thenReturn(true);
        when(postRepository.findByClub_IdAndStatus(clubId, PostStatus.PUBLISHED, pageable))
                .thenReturn(page);

        Page<PostWithRelationsData> result = postService.getClubFeed(clubId, userId, pageable);

        verify(clubMemberShipRepository, never())
                .findTeamIdsByUserAndClubAndStatus(anyLong(), anyLong(), any());
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    void getClubFeed_whenMemberHasNoTeam_shouldSeeClubWideOnly() {
        Long clubId = 2L;
        Long userId = 20L;
        Pageable pageable = PageRequest.of(0, 10);

        Post p1 = buildPost(2L, clubId, null, true, PostStatus.PUBLISHED);
        Page<Post> page = new PageImpl<>(List.of(p1), pageable, 1);

        when(clubRoleService.isClubLeaderOrVice(userId, clubId)).thenReturn(false);
        when(clubMemberShipRepository.findTeamIdsByUserAndClubAndStatus(
                eq(userId), eq(clubId), eq(ClubMemberShipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());
        when(postRepository.findClubWidePosts(clubId, true, PostStatus.PUBLISHED, pageable))
                .thenReturn(page);

        Page<PostWithRelationsData> result = postService.getClubFeed(clubId, userId, pageable);

        verify(postRepository, never()).findFeedForMemberInClub(anyLong(), anyString(), anyList(), any());
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isClubWide());
    }

    @Test
    void getClubFeed_whenMemberHasTeams_shouldSeeClubWideAndTeamPosts() {
        Long clubId = 3L;
        Long userId = 30L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Long> teamIds = List.of(11L, 12L);

        Post p1 = buildPost(3L, clubId, null, true, PostStatus.PUBLISHED);
        Post p2 = buildPost(4L, clubId, 11L, false, PostStatus.PUBLISHED);
        Page<Post> page = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(clubRoleService.isClubLeaderOrVice(userId, clubId)).thenReturn(false);
        when(clubMemberShipRepository.findTeamIdsByUserAndClubAndStatus(
                eq(userId), eq(clubId), eq(ClubMemberShipStatus.ACTIVE)))
                .thenReturn(teamIds);
        when(postRepository.findFeedForMemberInClub(clubId, PostStatus.PUBLISHED, teamIds, pageable))
                .thenReturn(page);

        Page<PostWithRelationsData> result = postService.getClubFeed(clubId, userId, pageable);

        assertEquals(2, result.getTotalElements());
    }

    // ====================
    // searchPostsInClub
    // ====================

    @Test
    void searchPostsInClub_whenKeywordBlank_shouldReturnEmptyPageAndNoRepoCall() {
        Long clubId = 1L;
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostWithRelationsData> result = postService.searchPostsInClub(
                clubId, userId, "   ", pageable);

        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(postRepository);
    }

    @Test
    void searchPostsInClub_whenAdmin_shouldUseAdminScope() {
        Long clubId = 5L;
        Long userId = 50L;
        String keyword = "event";
        Pageable pageable = PageRequest.of(0, 10);

        Post p1 = buildPost(10L, clubId, null, true, PostStatus.PUBLISHED);
        Post p2 = buildPost(11L, clubId, 100L, false, PostStatus.PUBLISHED);

        when(clubRoleService.isClubLeaderOrVice(userId, clubId)).thenReturn(true);
        when(postRepository.searchAdminScope(clubId, PostStatus.PUBLISHED, keyword.trim()))
                .thenReturn(List.of(p1, p2));

        Page<PostWithRelationsData> result = postService.searchPostsInClub(
                clubId, userId, keyword, pageable);

        verify(postRepository).searchAdminScope(clubId, PostStatus.PUBLISHED, keyword.trim());
        assertEquals(2, result.getTotalElements());
        assertEquals("Title 10", result.getContent().get(0).getTitle());
    }

    @Test
    void searchPostsInClub_whenMember_shouldMergeAndDistinctClubWideAndTeamPosts() {
        Long clubId = 6L;
        Long userId = 60L;
        String keyword = "meeting";
        Pageable pageable = PageRequest.of(0, 10);

        Post clubWidePost = buildPost(20L, clubId, null, true, PostStatus.PUBLISHED);
        Post teamPost = buildPost(21L, clubId, 200L, false, PostStatus.PUBLISHED);

        // duplicated để test distinct
        Post duplicated = buildPost(22L, clubId, 200L, false, PostStatus.PUBLISHED);

        when(clubRoleService.isClubLeaderOrVice(userId, clubId)).thenReturn(false);
        when(postRepository.searchClubWideOnly(clubId, PostStatus.PUBLISHED, keyword.trim()))
                .thenReturn(List.of(clubWidePost, duplicated));
        when(clubMemberShipRepository.findTeamIdsByUserAndClubAndStatus(
                eq(userId), eq(clubId), eq(ClubMemberShipStatus.ACTIVE)))
                .thenReturn(List.of(200L));
        when(postRepository.searchTeamScope(clubId, List.of(200L), PostStatus.PUBLISHED, keyword.trim()))
                .thenReturn(List.of(teamPost, duplicated));

        Page<PostWithRelationsData> result = postService.searchPostsInClub(
                clubId, userId, keyword, pageable);

        // distinct => 3 bài
        assertEquals(3, result.getTotalElements());
    }

    // ========================================
    // createPostWithUploads (không test upload)
    // ========================================

    @Test
    void createPostWithUploads_whenClubPresident_shouldAutoPublishAndApprove() {
        Long clubId = 1L;
        Long authorId = 99L;

        CreatePostRequest req = new CreatePostRequest();
        req.setClubId(clubId);
        req.setClubWide(true);         // bài toàn CLB
        req.setTeamId(123L);           // sẽ bị set null nếu clubWide=true
        req.setTitle("New Post");
        req.setContent("New Content");
        req.setWithinClub(true);
        req.setMedia(List.of());       // không test media ở đây

        Club clubRef = new Club();
        clubRef.setId(clubId);
        User authorRef = new User();
        authorRef.setId(authorId);
        authorRef.setFullName("President");

        when(clubRoleService.isClubLeaderOrVice(authorId, clubId)).thenReturn(true);
        //when(clubRoleService.isTeamLeader(anyLong(), anyLong())).thenReturn(false);

        when(em.getReference(Club.class, clubId)).thenReturn(clubRef);
        when(em.getReference(User.class, authorId)).thenReturn(authorRef);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1000L);
            return p;
        });

        PostWithRelationsData result = postService.createPostWithUploads(req, List.of(), authorId);

        assertEquals(1000L, result.getId());
        assertEquals(PostStatus.PUBLISHED, result.getStatus());
        assertEquals(authorId, result.getApprovedById());
        assertTrue(result.isClubWide());
        assertNull(result.getTeamId()); // teamId bị null vì clubWide = true
    }

    @Test
    void createPostWithUploads_whenMemberTeamLeadNonClubWide_shouldAutoPublish() {
        Long clubId = 2L;
        Long teamId = 10L;
        Long authorId = 200L;

        CreatePostRequest req = new CreatePostRequest();
        req.setClubId(clubId);
        req.setClubWide(false);
        req.setTeamId(teamId);
        req.setTitle("Team post");
        req.setContent("Team content");

        Club clubRef = new Club();
        clubRef.setId(clubId);
        Team teamRef = new Team();
        teamRef.setId(teamId);
        User authorRef = new User();
        authorRef.setId(authorId);

        when(clubRoleService.isClubLeaderOrVice(authorId, clubId)).thenReturn(false);
        when(clubRoleService.isTeamLeader(authorId, teamId)).thenReturn(true);

        when(em.getReference(Club.class, clubId)).thenReturn(clubRef);
        when(em.getReference(Team.class, teamId)).thenReturn(teamRef);
        when(em.getReference(User.class, authorId)).thenReturn(authorRef);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(2000L);
            return p;
        });

        PostWithRelationsData result = postService.createPostWithUploads(req, List.of(), authorId);

        assertEquals(PostStatus.PUBLISHED, result.getStatus());
        assertEquals(teamId, result.getTeamId());
    }

    // ======================================
    // updatePostWithUploads (case đơn giản)
    // ======================================

    @Test
    void updatePostWithUploads_whenPendingClubWideAndClubBoss_shouldPublish() {
        Long postId = 1L;
        Long clubId = 10L;
        Long authorId = 100L;

        Post existing = buildPost(postId, clubId, null, true, PostStatus.PENDING);

        UpdatePostRequest req = new UpdatePostRequest();
        req.setTitle("Updated title");
        req.setContent("Updated content");

        when(postRepository.findById(postId)).thenReturn(Optional.of(existing));
        when(clubRoleService.isClubLeaderOrVice(authorId, clubId)).thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostWithRelationsData result = postService.updatePostWithUploads(postId, req, List.of(), authorId);

        assertEquals("Updated title", result.getTitle());
        assertEquals(PostStatus.PUBLISHED, result.getStatus());
    }

    // ==================
    // deletePost
    // ==================

    @Test
    void deletePost_shouldDeleteExistingPost() {
        Long postId = 100L;
        Post p = buildPost(postId, 1L, null, true, PostStatus.PUBLISHED);

        when(postRepository.findById(postId)).thenReturn(Optional.of(p));

        postService.deletePost(postId);

        verify(postRepository).delete(p);
    }

    // ==================
    // deleteOneMedia
    // ==================

    @Test
    void deleteOneMedia_shouldRemoveMediaAndReindex() {
        Long postId = 1L;
        Long mediaIdToDelete = 2L;

        Post p = buildPost(postId, 1L, null, true, PostStatus.PUBLISHED);

        PostMedia m1 = new PostMedia();
        m1.setId(1L);
        m1.setDisplayOrder(0);
        m1.setPost(p);

        PostMedia m2 = new PostMedia();
        m2.setId(mediaIdToDelete);
        m2.setDisplayOrder(1);
        m2.setPost(p);

        PostMedia m3 = new PostMedia();
        m3.setId(3L);
        m3.setDisplayOrder(2);
        m3.setPost(p);

        Set<PostMedia> mediaSet = new LinkedHashSet<>();
        mediaSet.add(m1);
        mediaSet.add(m2);
        mediaSet.add(m3);
        p.setPostMedia(mediaSet);

        when(postRepository.findById(postId)).thenReturn(Optional.of(p));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostWithRelationsData result = postService.deleteOneMedia(postId, mediaIdToDelete);

        verify(em).remove(m2);
        assertEquals(2, result.getMedia().size());

        // Sau reindex, displayOrder phải là 0,1
        assertEquals(0, result.getMedia().get(0).getDisplayOrder());
        assertEquals(1, result.getMedia().get(1).getDisplayOrder());
    }

    // ==================
    // canApprove
    // ==================

    @Test
    void canApprove_whenClubWideAndUserIsClubBoss_shouldReturnTrue() {
        Post p = buildPost(1L, 1L, null, true, PostStatus.PENDING);
        Long userId = 999L;

        when(clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId()))
                .thenReturn(true);

        assertTrue(postService.canApprove(userId, p));
    }

    @Test
    void canApprove_whenTeamPostAndUserIsTeamLeader_shouldReturnTrue() {
        Post p = buildPost(1L, 1L, 5L, false, PostStatus.PENDING);
        Long userId = 999L;

        when(clubRoleService.isTeamLeader(userId, p.getTeam().getId())).thenReturn(true);
        when(clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId())).thenReturn(false);

        assertTrue(postService.canApprove(userId, p));
    }

    @Test
    void canApprove_whenTeamPostAndUserIsClubBoss_shouldReturnTrue() {
        Post p = buildPost(1L, 1L, 5L, false, PostStatus.PENDING);
        Long userId = 999L;

        when(clubRoleService.isTeamLeader(userId, p.getTeam().getId())).thenReturn(false);
        when(clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId())).thenReturn(true);

        assertTrue(postService.canApprove(userId, p));
    }

    @Test
    void canApprove_whenNoRole_shouldReturnFalse() {
        Post p = buildPost(1L, 1L, 5L, false, PostStatus.PENDING);
        Long userId = 999L;

        when(clubRoleService.isTeamLeader(userId, p.getTeam().getId())).thenReturn(false);
        when(clubRoleService.isClubLeaderOrVice(userId, p.getClub().getId())).thenReturn(false);

        assertFalse(postService.canApprove(userId, p));
    }
}
