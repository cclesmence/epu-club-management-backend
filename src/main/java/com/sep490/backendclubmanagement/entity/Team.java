package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, length = 200)
    private String teamName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "link_group_chat", nullable = true, length = 200)
    private String linkGroupChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private Set<RoleMemberShip> roleMemberships;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private Set<Post> posts;
}

