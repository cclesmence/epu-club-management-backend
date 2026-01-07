package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.EventRepository;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import com.sep490.backendclubmanagement.service.HomepageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomepageServiceTest {

    @Mock private ClubRepository clubRepository;
    @Mock private EventRepository eventRepository;
    @Mock private NewsRepository newsRepository;

    @InjectMocks private HomepageService homepageService;

    private FeaturedClubDTO featuredClub;
    private UpcomingEventDTO upcomingEvent;
    private LatestNewsDTO latestNews;

    @BeforeEach
    void setup() {
        featuredClub = new FeaturedClubDTO(
                1L,
                "DEV Club",
                "logo.png",
                "Club description"
        );

        upcomingEvent = new UpcomingEventDTO(
                10L,
                "Hackathon",
                LocalDateTime.now().plusDays(1),
                "Hall A",
                "DEV Club",
                "event.png"
        );

        latestNews = new LatestNewsDTO(
                100L,
                "New Project Launch",
                "thumb.png",
                "Excerpt text...",
                LocalDateTime.now()
        );
    }

    @Test
    void getHomepageData_success() {
        // Mock STEP 1
        doNothing().when(clubRepository).resetAllFeatured();
        when(clubRepository.findTopClubIdsByEventCount(any(PageRequest.class)))
                .thenReturn(List.of(1L, 2L));
        doNothing().when(clubRepository).updateFeaturedClubs(anyList());

        // STEP 2: Featured clubs
        when(clubRepository.findFeaturedClubs())
                .thenReturn(List.of(featuredClub));

        // STEP 3: Upcoming events
        when(eventRepository.findUpcomingEvents(any(), any(PageRequest.class)))
                .thenReturn(List.of(upcomingEvent));

        // STEP 4: Latest news
        when(newsRepository.findLatestNews(any(PageRequest.class)))
                .thenReturn(List.of(latestNews));

        // STEP 4.5 spotlight
        Club mockNews = new Club(); // <-- fake entity chỉ để getId
        when(newsRepository.findTopByIsDraftFalseOrderByCreatedAtDesc())
                .thenReturn(Optional.empty());
        when(newsRepository.findTopByIsSpotlightTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.empty());

        // Act
        HomepageResponse resp = homepageService.getHomepageData();

        // Assert
        assertNotNull(resp);

        // Featured
        assertEquals(1, resp.getFeaturedClubs().size());
        assertEquals("DEV Club", resp.getFeaturedClubs().get(0).getClubName());

        // Upcoming events
        assertEquals(1, resp.getUpcomingEvents().size());
        assertEquals("Hackathon", resp.getUpcomingEvents().get(0).getTitle());

        // Latest news
        assertEquals(1, resp.getLatestNews().size());
        assertEquals("New Project Launch", resp.getLatestNews().get(0).getTitle());

        // Spotlight (empty)
        assertNull(resp.getSpotlight());

        // Verify internal behavior
        verify(clubRepository, times(1)).resetAllFeatured();
        verify(clubRepository, times(1)).updateFeaturedClubs(anyList());
        verify(eventRepository, times(1)).findUpcomingEvents(any(), any(PageRequest.class));
        verify(newsRepository, times(1)).findLatestNews(any(PageRequest.class));
    }
}
