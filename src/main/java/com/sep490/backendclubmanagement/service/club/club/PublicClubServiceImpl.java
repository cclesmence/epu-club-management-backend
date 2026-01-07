package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.dto.response.PageResp;
import com.sep490.backendclubmanagement.dto.response.PublicClubCardDTO;
import com.sep490.backendclubmanagement.dto.response.PublicClubDetailDTO;
import com.sep490.backendclubmanagement.dto.response.PublicEventDTO;
import com.sep490.backendclubmanagement.dto.response.PublicNewsDTO;
import com.sep490.backendclubmanagement.dto.response.PublicTeamDTO;
import com.sep490.backendclubmanagement.repository.PublicClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicClubServiceImpl implements PublicClubService {

    private final PublicClubRepository repo;

    @Override
    public PageResp<PublicClubCardDTO> list(String q, Long campusId, Long categoryId, int page, int size) {
        Page<PublicClubRepository.ClubCardRow> rows =
                repo.findPublicClubs(normalize(q), campusId, categoryId, PageRequest.of(page, size));

        List<PublicClubCardDTO> content = rows.getContent().stream().map(r -> {
            List<String> topTags = splitCsv2(r.getTop2_names());
            int overflow = (int) Math.max(0, Optional.ofNullable(r.getTotal_teams()).orElse(0L) - topTags.size());
            Long activeRecruitmentId = r.getActive_recruitment_id();
            return PublicClubCardDTO.builder()
                    .id(r.getId())
                    .clubCode(r.getClub_code())
                    .clubName(r.getClub_name())
                    .shortDescription(r.getShort_description())
                    .bannerUrl(r.getBanner_url())
                    .logoUrl(r.getLogo_url())
                    .featured(Boolean.TRUE.equals(r.getIs_featured()))
                    .createdAt(r.getCreated_at())
                    .categoryName(r.getCategory_name())
                    .campusName(r.getCampus_name())
                    .totalTeams(r.getTotal_teams())
                    .topTags(topTags)
                    .tagsOverflow(overflow)
                    .hasActiveRecruitment(activeRecruitmentId != null)
                    .activeRecruitmentId(activeRecruitmentId)
                    .build();
        }).toList();

        return PageResp.<PublicClubCardDTO>builder()
                .content(content)
                .totalElements(rows.getTotalElements())
                .totalPages(rows.getTotalPages())
                .number(rows.getNumber())
                .size(rows.getSize())
                .first(rows.isFirst())
                .last(rows.isLast())
                .build();
    }

    @Override
    public PublicClubDetailDTO detail(Long clubId, String expand) {
        PublicClubRepository.ClubDetailRow r = repo.findPublicClubDetail(clubId)
                .orElseThrow(() -> new NoSuchElementException("Club not found or inactive"));

        PublicClubDetailDTO dto = PublicClubDetailDTO.builder()
                .id(r.getId())
                .clubCode(r.getClub_code())
                .clubName(r.getClub_name())
                .description(r.getDescription())
                .bannerUrl(r.getBanner_url())
                .logoUrl(r.getLogo_url())
                .email(r.getEmail())
                .phone(r.getPhone())
                .fbUrl(r.getFb_url())
                .igUrl(r.getIg_url())
                .ttUrl(r.getTt_url())
                .ytUrl(r.getYt_url())
                .campusName(r.getCampus_name())
                .categoryName(r.getCategory_name())
                .createdAt(r.getCreated_at())
                .updatedAt(r.getUpdated_at())
                .featured(Boolean.TRUE.equals(r.getIs_featured()))
                .departmentsCount(Optional.ofNullable(r.getDepartments_count()).orElse(0L))
                .establishedYear(r.getCreated_at() == null ? null : r.getCreated_at().getYear())
                .build();

        Set<String> ex = parseExpand(expand);

        if (ex.contains("teams")) {
            var teams = repo.findPublicTeamsWithLeaderAndCount(clubId).stream()
                    .map(t -> PublicTeamDTO.builder()
                            .id(t.getId())
                            .teamName(t.getTeam_name())
                            .description(t.getDescription())
                            .linkGroupChat(t.getLink_group_chat())
                            .leaderName(t.getLeader_name())
                            .memberCount(Optional.ofNullable(t.getMember_count()).orElse(0))
                            .build())
                    .toList();

            dto.setTeams(teams);
        }

        if (ex.contains("events")) {
            var events = repo.findUpcomingEvents(clubId, PageRequest.of(0, 5)).stream()
                    .map(e -> PublicEventDTO.builder()
                            .id(e.getId())
                            .title(e.getTitle())
                            .description(e.getDescription())
                            .startTime(e.getStart_time())
                            .endTime(e.getEnd_time())
                            .location(e.getLocation())
                            .build())
                    .toList();
            dto.setEvents(events);
        }

        if (ex.contains("news")) {
            var news = repo.findLatestNews(clubId, PageRequest.of(0, 6)).stream()
                    .map(n -> PublicNewsDTO.builder()
                            .id(n.getId())
                            .title(n.getTitle())
                            .thumbnailUrl(n.getThumbnail_url())
                            .excerpt(n.getExcerpt())
                            .publishedAt(n.getPublished_at())
                            .spotlight(n.getIs_spotlight())
                            .build())
                    .toList();
            dto.setNews(news);
        }

        return dto;
    }

    /* ---------------- helpers ---------------- */

    private String normalize(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private Set<String> parseExpand(String expand) {
        if (!StringUtils.hasText(expand)) return Collections.emptySet();
        return Arrays.stream(expand.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private List<String> splitCsv2(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(2)
                .toList();
    }
}
