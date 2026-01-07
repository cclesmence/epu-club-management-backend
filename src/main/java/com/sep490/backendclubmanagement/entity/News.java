package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name="is_spotlight")
    private Boolean isSpotlight = false;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "news_type", length = 100)
    private String newsType;


    @Column(name = "is_draft")
    private Boolean isDraft = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = true)
    private Club club;

    @OneToOne(mappedBy = "news", cascade = CascadeType.ALL)
    private RequestNews requestNews;

    //update delete xóa mềm news
    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_by_id")
    private Long deletedById;

    @Column(name = "updated_by_id")
    private Long updatedById;
}

