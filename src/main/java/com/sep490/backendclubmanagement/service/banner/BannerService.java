package com.sep490.backendclubmanagement.service.banner;

import com.sep490.backendclubmanagement.dto.request.BannerUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.BannerResponse;
import com.sep490.backendclubmanagement.entity.BannerConfig;
import com.sep490.backendclubmanagement.repository.BannerConfigRepository;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerConfigRepository repo;
    private final WebSocketService ws;

    public BannerResponse get() {
        return toResponse(repo.findById(1L).orElseThrow());
    }

    @Transactional
    public BannerResponse update(BannerUpdateRequest req) {
        BannerConfig c = repo.findById(1L).orElseThrow();

        c.setTitle(req.title());
        c.setSubtitle(req.subtitle());
        c.setImageUrl(req.imageUrl());
        c.setCtaLabel(req.ctaLabel());
        c.setCtaLink(req.ctaLink());
        repo.save(c);

        ws.broadcastSystemWide("BANNER", "UPDATED", null);
        return toResponse(c);
    }

    private BannerResponse toResponse(BannerConfig c) {
        return new BannerResponse(
                c.getId(), c.getTitle(), c.getSubtitle(),
                c.getImageUrl(), c.getCtaLabel(), c.getCtaLink()
        );
    }
}
