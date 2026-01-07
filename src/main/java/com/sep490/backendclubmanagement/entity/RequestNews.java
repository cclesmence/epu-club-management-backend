package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestNews extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_title", nullable = false, length = 255)
    private String requestTitle;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "response_message", nullable = false, columnDefinition = "TEXT")
    private String responseMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private RequestStatus status;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="semester_id")
    private Semester semester;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", unique = true)
    private News news;
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "news_type", length = 100)
    private String newsType;
    @PrePersist
    void prePersist() {
        if (requestDate == null) requestDate = LocalDateTime.now();
        if (responseMessage == null) responseMessage = "";
    }

}

