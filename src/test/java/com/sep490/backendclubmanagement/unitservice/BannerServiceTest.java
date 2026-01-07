package com.sep490.backendclubmanagement.service;

import com.sep490.backendclubmanagement.dto.request.BannerUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.BannerResponse;
import com.sep490.backendclubmanagement.entity.BannerConfig;
import com.sep490.backendclubmanagement.repository.BannerConfigRepository;
import com.sep490.backendclubmanagement.service.banner.BannerService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerConfigRepository repo;

    @Mock
    private WebSocketService ws;

    @InjectMocks
    private BannerService service;

    private BannerConfig config;

    @BeforeEach
    void setup() {
        config = new BannerConfig();
        config.setId(1L);
        config.setTitle("Old title");
        config.setSubtitle("Old subtitle");
        config.setImageUrl("old.png");
        config.setCtaLabel("Old CTA");
        config.setCtaLink("http://old");
    }

    // =====================================
    // GET
    // =====================================
    @Test
    void get_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(config));

        BannerResponse res = service.get();

        assertEquals(1L, res.id());
        assertEquals("Old title", res.title());
        assertEquals("old.png", res.imageUrl());
        verify(repo).findById(1L);
    }

    @Test
    void get_notFound_throwsException() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.get());
    }

    // =====================================
    // UPDATE
    // =====================================
    @Test
    void update_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(config));

        BannerUpdateRequest req = new BannerUpdateRequest(
                "New Title",
                "New Subtitle",
                "new.png",
                "Join Now",
                "http://new"
        );

        BannerResponse res = service.update(req);

        // verify updated values
        assertEquals("New Title", config.getTitle());
        assertEquals("New Subtitle", config.getSubtitle());
        assertEquals("new.png", config.getImageUrl());
        assertEquals("Join Now", config.getCtaLabel());
        assertEquals("http://new", config.getCtaLink());

        // repo.save được gọi
        verify(repo).save(config);

        // websocket broadcast được gọi
        verify(ws).broadcastSystemWide("BANNER", "UPDATED", null);

        // response khớp
        assertEquals("New Title", res.title());
    }

    @Test
    void update_notFound_throwsException() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        BannerUpdateRequest req = new BannerUpdateRequest(
                "T", "S", "i", "c", "l"
        );

        assertThrows(Exception.class, () -> service.update(req));
    }
}
