package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import com.sep490.backendclubmanagement.entity.recruitment.Recruitment;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 200)
    private String clubName;

    @Column(name = "club_code", unique = true, length = 50)
    private String clubCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "fb_url", length = 500)
    private String fbUrl;
    @Column(name = "ig_url", length = 500)
    private String igUrl;
    @Column(name = "tt_url", length = 500)
    private String ttUrl;
    @Column(name = "yt_url", length = 500)
    private String ytUrl;

    @Column(name = "status", length = 50)
    private String status;

    // ----- BẠN CHỈ CẦN THÊM 2 DÒNG NÀY VÀO -----
    @Column(name = "is_featured")
    private boolean isFeatured = false;
    // ---------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_category_id")
    private ClubCategory clubCategory;

    // ... các trường còn lại giữ nguyên ...
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<ClubMemberShip> clubMemberships;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<Post> posts;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<Team> teams;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<Event> events;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<News> news;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<ClubRole> clubRoles;

    @OneToOne(mappedBy = "club", cascade = CascadeType.ALL)
    private ClubWallet clubWallet;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<Recruitment> recruitments;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<ClubProposal> clubProposals;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<Fee> fees;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private Set<ClubReportRequirement> clubReportRequirements;
}