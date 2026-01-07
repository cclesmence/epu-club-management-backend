package com.sep490.backendclubmanagement.service;

import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.EventRepository;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomepageService {

    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;

    @Transactional
    public HomepageResponse getHomepageData() {

        // 🔹 STEP 1: Cập nhật danh sách CLB nổi bật (top 5 có nhiều event nhất)
        clubRepository.resetAllFeatured();
        List<Long> topClubIds = clubRepository.findTopClubIdsByEventCount(PageRequest.of(0, 5));
        if (!topClubIds.isEmpty()) {
            clubRepository.updateFeaturedClubs(topClubIds);
        }

        // 🔹 STEP 2: Lấy 4 CLB nổi bật (sau khi auto cập nhật)
        List<FeaturedClubDTO> featuredClubs = clubRepository.findFeaturedClubs().stream()
                .limit(4)
                .toList();

        // 🔹 STEP 3: Sự kiện sắp diễn ra (4)
        List<UpcomingEventDTO> upcomingEvents = eventRepository.findUpcomingEvents(
                LocalDateTime.now(), PageRequest.of(0, 4));

        // 🔹 STEP 4: Tin tức mới nhất (4)
        List<LatestNewsDTO> latestNews = newsRepository.findLatestNews(PageRequest.of(0, 4));
        newsRepository.findTopByIsDraftFalseOrderByCreatedAtDesc().ifPresent(latest -> {
            newsRepository.clearAllSpotlight();
            newsRepository.markSpotlight(latest.getId());
        });
        // 🔹 STEP 5: Spotlight (1 tin nổi bật nhất)
        SpotlightDTO spotlight = newsRepository.findTopByIsSpotlightTrueOrderByCreatedAtDesc()
                .map(news -> SpotlightDTO.builder()
                        .type(news.getNewsType())
                        .title(news.getTitle())
                        .description(
                                news.getContent() == null ? "" :
                                        (news.getContent().length() > 150
                                                ? news.getContent().substring(0, 150) + "..."
                                                : news.getContent())
                        )
                        .imageUrl(news.getThumbnailUrl())
                        .callToActionText("Read more")
                        .callToActionLink("/news/" + news.getId())
                        .build())
                .orElse(null);

        // 🔹 STEP 6: Trả về tất cả dữ liệu
        return new HomepageResponse(featuredClubs, upcomingEvents, latestNews, spotlight);
    }
}
