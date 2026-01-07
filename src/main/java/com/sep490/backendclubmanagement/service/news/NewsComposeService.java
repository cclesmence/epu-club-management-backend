package com.sep490.backendclubmanagement.service.news;

import com.sep490.backendclubmanagement.dto.request.CreateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.dto.response.PublishResult;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.mapper.NewsMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsComposeService {

    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final NewsRepository newsRepo;
    private final RequestNewsRepository requestRepo;
    private final RoleGuard guard;
    private final NewsMapper mapper;

    @Transactional
    public NewsData createAsDraft(Long me, CreateNewsRequest body) {
        User creator = userRepo.findById(me).orElseThrow();
        Club club = null;
        if (!guard.isStaff(me)) {
            if (body.getClubId() == null) throw new IllegalArgumentException("Thiếu clubId cho vai trò CLB");
            club = clubRepo.findById(body.getClubId()).orElseThrow();
            if (!guard.canCreateNews(me, club.getId())) {
                throw new SecurityException("Không có quyền tạo bài cho CLB này");
            }
        }
        News news = News.builder()
                .title(body.getTitle())
                .content(body.getContent())
                .thumbnailUrl(body.getThumbnailUrl())
                .newsType(body.getNewsType())
                .isSpotlight(Boolean.TRUE.equals(body.getIsSpotlight()))
                .isDraft(true)
                .createdBy(creator)
                .club(guard.isStaff(me) ? null : club)
                .build();
        newsRepo.save(news);
        return mapper.toDto(news);
    }

    @Transactional
    public Object createAndSubmitRequest(Long me, CreateNewsRequest body) {
        // 1) tạo nháp trước
        NewsData draft = createAsDraft(me, body);
        // 2) submit nháp thành request
        RequestStatus start = guard.isStaff(me) ? RequestStatus.PENDING_UNIVERSITY : RequestStatus.PENDING_CLUB;

        User actor = userRepo.findById(me).orElseThrow();
        News news = newsRepo.findById(draft.getId()).orElseThrow();

        RequestNews req = RequestNews.builder()
                .requestTitle(news.getTitle())
                .description(news.getContent())
                .status(start)
                .createdBy(actor)
                .club(news.getClub()) // staff sẽ là null
                .news(news)
                .build();
        requestRepo.save(req);
        return java.util.Map.of(
                "requestId", req.getId(),
                "status", req.getStatus().name(),
                "newsId", news.getId()
        );
    }

    @Transactional
    public PublishResult createAndPublishByStaff(Long me, CreateNewsRequest body) {
        if (!guard.isStaff(me)) throw new SecurityException("Chỉ Staff được publish trực tiếp.");
        User staff = userRepo.findById(me).orElseThrow();

        News news = News.builder()
                .title(body.getTitle())
                .content(body.getContent())
                .thumbnailUrl(body.getThumbnailUrl())
                .newsType(body.getNewsType())
                .isSpotlight(Boolean.TRUE.equals(body.getIsSpotlight()))
                .isDraft(false)
                .createdBy(staff)
                .club(null)
                .build();
        newsRepo.save(news);

        return new PublishResult(news.getId(), mapper.toDto(news),"Đăng trực tiếp thành công");
    }
}
