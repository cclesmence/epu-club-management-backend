package com.sep490.backendclubmanagement.dto.response;

public record BannerResponse(
        Long id,
        String title,
        String subtitle,
        String imageUrl,
        String ctaLabel,
        String ctaLink
) {}
