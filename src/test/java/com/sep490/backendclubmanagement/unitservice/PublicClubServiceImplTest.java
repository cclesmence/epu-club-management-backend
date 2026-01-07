package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.PageResp;
import com.sep490.backendclubmanagement.dto.response.PublicClubCardDTO;
import com.sep490.backendclubmanagement.dto.response.PublicClubDetailDTO;
import com.sep490.backendclubmanagement.repository.PublicClubRepository;
import com.sep490.backendclubmanagement.repository.PublicClubRepository.ClubCardRow;
import com.sep490.backendclubmanagement.repository.PublicClubRepository.ClubDetailRow;
import com.sep490.backendclubmanagement.repository.PublicClubRepository.TeamWithLeaderRow;
import com.sep490.backendclubmanagement.repository.PublicClubRepository.EventRow;
import com.sep490.backendclubmanagement.repository.PublicClubRepository.NewsRow;
import com.sep490.backendclubmanagement.service.club.club.PublicClubServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PublicClubServiceImplTest {

    @Mock
    private PublicClubRepository repo;

    @InjectMocks
    private PublicClubServiceImpl publicClubService;

    // ========================= list() =========================

    @Test
    void list_happyPath_mapsRowsToDto() {
        // Arrange
        String q = "  dev club  ";
        Long campusId = 1L;
        Long categoryId = 2L;
        int page = 0;
        int size = 10;

        ClubCardRow row = mock(ClubCardRow.class);
        when(row.getId()).thenReturn(10L);
        when(row.getClub_code()).thenReturn("DEV");
        when(row.getClub_name()).thenReturn("Dev Club");
        when(row.getShort_description()).thenReturn("Short desc");
        when(row.getBanner_url()).thenReturn("banner.png");
        when(row.getLogo_url()).thenReturn("logo.png");
        when(row.getIs_featured()).thenReturn(Boolean.TRUE);
        when(row.getCreated_at()).thenReturn(LocalDateTime.now());
        when(row.getCategory_name()).thenReturn("Tech");
        when(row.getCampus_name()).thenReturn("FPT HN");
        when(row.getTotal_teams()).thenReturn(4L);
        when(row.getTop2_names()).thenReturn("Ban Kỹ thuật,Ban Media");
        when(row.getActive_recruitment_id()).thenReturn(99L);

        PageRequest pr = PageRequest.of(page, size);
        Page<ClubCardRow> rowsPage = new PageImpl<>(List.of(row), pr, 1);

        when(repo.findPublicClubs(eq("dev club"), eq(campusId), eq(categoryId), any(PageRequest.class)))
                .thenReturn(rowsPage);

        // Act
        PageResp<PublicClubCardDTO> resp =
                publicClubService.list(q, campusId, categoryId, page, size);

        // Assert
        assertNotNull(resp);
        assertEquals(1, resp.getTotalElements());
        assertEquals(1, resp.getContent().size());

        PublicClubCardDTO dto = resp.getContent().get(0);
        assertEquals(10L, dto.getId());
        assertEquals("DEV", dto.getClubCode());
        assertEquals("Dev Club", dto.getClubName());
        assertEquals("Short desc", dto.getShortDescription());
        assertEquals("banner.png", dto.getBannerUrl());
        assertEquals("logo.png", dto.getLogoUrl());
        assertEquals("Tech", dto.getCategoryName());
        assertEquals("FPT HN", dto.getCampusName());
        assertTrue(dto.isFeatured());
        assertEquals(4L, dto.getTotalTeams());
        assertEquals(List.of("Ban Kỹ thuật", "Ban Media"), dto.getTopTags());
        assertEquals(2, dto.getTagsOverflow()); // 4 teams - 2 tags
        assertTrue(dto.getHasActiveRecruitment());
        assertEquals(99L, dto.getActiveRecruitmentId());
    }

    @Test
    void list_noQuery_returnsEmptyPageWhenRepoReturnsEmpty() {
        // Arrange
        int page = 0;
        int size = 5;
        PageRequest pr = PageRequest.of(page, size);
        Page<ClubCardRow> rowsPage = new PageImpl<>(List.of(), pr, 0);

        when(repo.findPublicClubs(isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(rowsPage);

        // Act
        PageResp<PublicClubCardDTO> resp =
                publicClubService.list(null, null, null, page, size);

        // Assert
        assertNotNull(resp);
        assertEquals(0, resp.getTotalElements());
        assertTrue(resp.getContent().isEmpty());
    }

    // ========================= detail() =========================

    @Test
    void detail_basic_happyPath_withoutExpand() {
        // Arrange
        Long clubId = 10L;

        ClubDetailRow row = mock(ClubDetailRow.class);
        when(row.getId()).thenReturn(clubId);
        when(row.getClub_code()).thenReturn("DEV");
        when(row.getClub_name()).thenReturn("Dev Club");
        when(row.getDescription()).thenReturn("Full desc");
        when(row.getBanner_url()).thenReturn("banner.png");
        when(row.getLogo_url()).thenReturn("logo.png");
        when(row.getEmail()).thenReturn("dev@club.com");
        when(row.getPhone()).thenReturn("0123");
        when(row.getFb_url()).thenReturn("fb.com/dev");
        when(row.getIg_url()).thenReturn("ig.com/dev");
        when(row.getTt_url()).thenReturn("tt.com/dev");
        when(row.getYt_url()).thenReturn("yt.com/dev");
        when(row.getCampus_name()).thenReturn("FPT HN");
        when(row.getCategory_name()).thenReturn("Tech");
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        when(row.getCreated_at()).thenReturn(createdAt);
        when(row.getUpdated_at()).thenReturn(createdAt.plusDays(1));
        when(row.getIs_featured()).thenReturn(Boolean.TRUE);
        when(row.getDepartments_count()).thenReturn(3L);

        when(repo.findPublicClubDetail(clubId))
                .thenReturn(Optional.of(row));

        // Act
        PublicClubDetailDTO dto = publicClubService.detail(clubId, null);

        // Assert
        assertNotNull(dto);
        assertEquals(clubId, dto.getId());
        assertEquals("DEV", dto.getClubCode());
        assertEquals("Dev Club", dto.getClubName());
        assertEquals("Full desc", dto.getDescription());
        assertEquals("banner.png", dto.getBannerUrl());
        assertEquals("logo.png", dto.getLogoUrl());
        assertEquals("dev@club.com", dto.getEmail());
        assertEquals("0123", dto.getPhone());
        assertEquals("Tech", dto.getCategoryName());
        assertEquals("FPT HN", dto.getCampusName());
        assertEquals(3L, dto.getDepartmentsCount());
        assertTrue(dto.isFeatured());
        assertEquals((Integer) createdAt.getYear(), dto.getEstablishedYear());

        assertNull(dto.getTeams());
        assertNull(dto.getEvents());
        assertNull(dto.getNews());
    }

    @Test
    void detail_withExpandTeamsEventsNews_populatesExtraData() {
        // Arrange
        Long clubId = 10L;
        String expand = "teams, events , news";

        ClubDetailRow row = mock(ClubDetailRow.class);
        when(row.getId()).thenReturn(clubId);
        when(row.getClub_code()).thenReturn("DEV");
        when(row.getClub_name()).thenReturn("Dev Club");
        when(row.getDescription()).thenReturn("Desc");
        when(row.getCreated_at()).thenReturn(LocalDateTime.now());
        when(row.getDepartments_count()).thenReturn(0L);
        when(row.getIs_featured()).thenReturn(Boolean.FALSE);

        when(repo.findPublicClubDetail(clubId))
                .thenReturn(Optional.of(row));

        // teams
        TeamWithLeaderRow teamRow = mock(TeamWithLeaderRow.class);
        when(teamRow.getId()).thenReturn(1L);
        when(teamRow.getTeam_name()).thenReturn("Ban Kỹ thuật");
        when(teamRow.getDescription()).thenReturn("Desc");
        when(teamRow.getLink_group_chat()).thenReturn("link");
        when(teamRow.getLeader_name()).thenReturn("Leader");
        when(teamRow.getMember_count()).thenReturn(10);

        when(repo.findPublicTeamsWithLeaderAndCount(clubId))
                .thenReturn(List.of(teamRow));

        // events
        EventRow eventRow = mock(EventRow.class);
        when(eventRow.getId()).thenReturn(2L);
        when(eventRow.getTitle()).thenReturn("Event");
        when(eventRow.getDescription()).thenReturn("Event desc");
        when(eventRow.getStart_time()).thenReturn(LocalDateTime.now().plusDays(1));
        when(eventRow.getEnd_time()).thenReturn(LocalDateTime.now().plusDays(1).plusHours(2));
        when(eventRow.getLocation()).thenReturn("Hall A");

        when(repo.findUpcomingEvents(eq(clubId), any(PageRequest.class)))
                .thenReturn(List.of(eventRow));

        // news
        NewsRow newsRow = mock(NewsRow.class);
        when(newsRow.getId()).thenReturn(3L);
        when(newsRow.getTitle()).thenReturn("Tin mới");
        when(newsRow.getThumbnail_url()).thenReturn("thumb.png");
        when(newsRow.getExcerpt()).thenReturn("excerpt");
        when(newsRow.getPublished_at()).thenReturn(LocalDateTime.now());
        when(newsRow.getIs_spotlight()).thenReturn(Boolean.TRUE);

        when(repo.findLatestNews(eq(clubId), any(PageRequest.class)))
                .thenReturn(List.of(newsRow));

        // Act
        PublicClubDetailDTO dto = publicClubService.detail(clubId, expand);

        // Assert
        assertNotNull(dto);

        // teams
        assertNotNull(dto.getTeams());
        assertEquals(1, dto.getTeams().size());
        assertEquals("Ban Kỹ thuật", dto.getTeams().get(0).getTeamName());
        assertEquals("Leader", dto.getTeams().get(0).getLeaderName());
        assertEquals(10, dto.getTeams().get(0).getMemberCount());

        // events
        assertNotNull(dto.getEvents());
        assertEquals(1, dto.getEvents().size());
        assertEquals("Event", dto.getEvents().get(0).getTitle());
        assertEquals("Hall A", dto.getEvents().get(0).getLocation());

        // news
        assertNotNull(dto.getNews());
        assertEquals(1, dto.getNews().size());
        assertEquals("Tin mới", dto.getNews().get(0).getTitle());
        assertEquals("thumb.png", dto.getNews().get(0).getThumbnailUrl());
        assertTrue(dto.getNews().get(0).getSpotlight());
    }

    @Test
    void detail_clubNotFound_throwsNoSuchElementException() {
        // Arrange
        Long clubId = 999L;
        when(repo.findPublicClubDetail(clubId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> publicClubService.detail(clubId, null)
        );
    }
}
