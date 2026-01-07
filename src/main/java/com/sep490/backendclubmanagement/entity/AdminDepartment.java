package com.sep490.backendclubmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDepartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(name = "department_code", unique = true, length = 50)
    private String departmentCode;

    @Column(name = "sort_description", columnDefinition = "TEXT")
    private String sortDescription;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "fb_link", length = 500)
    private String fbLink;

    @Column(name = "ig_link", length = 500)
    private String igLink;

    @Column(name = "tt_link", length = 500)
    private String ttLink;

    @Column(name = "yt_link", length = 500)
    private String ytLink;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;
}

