package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomepageResponse {
    private List<FeaturedClubDTO> featuredClubs;
    private List<UpcomingEventDTO> upcomingEvents;
    private List<LatestNewsDTO> latestNews;
    private SpotlightDTO spotlight;
}