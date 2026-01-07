package com.sep490.backendclubmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "banner_config")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BannerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // luôn là 1

    private String title;
    private String subtitle;
    private String imageUrl;
    private String ctaLabel;
    private String ctaLink;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
