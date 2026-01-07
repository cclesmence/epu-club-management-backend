package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_memberships", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"club_membership_id", "semester_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMemberShip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_membership_id", nullable = false)
    private ClubMemberShip clubMemberShip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clubrole_id")
    private ClubRole clubRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

