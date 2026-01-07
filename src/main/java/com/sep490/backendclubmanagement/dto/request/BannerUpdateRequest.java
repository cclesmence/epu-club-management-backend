package com.sep490.backendclubmanagement.dto.request;

public record BannerUpdateRequest(
        String title,
        String subtitle,
        String imageUrl,
        String ctaLabel,
        String ctaLink
) {}
