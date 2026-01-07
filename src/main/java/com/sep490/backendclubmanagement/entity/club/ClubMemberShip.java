package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.RoleMemberShip;
import com.sep490.backendclubmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "club_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMemberShip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;



    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private ClubMemberShipStatus status;

    @OneToMany(mappedBy = "clubMemberShip", cascade = CascadeType.ALL)
    private Set<RoleMemberShip> roleMemberships;
}

