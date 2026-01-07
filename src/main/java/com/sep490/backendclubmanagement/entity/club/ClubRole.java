package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.RoleMemberShip;
import com.sep490.backendclubmanagement.entity.SystemRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "club_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "role_code", nullable = false, length = 100)
    private String roleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "role_level", nullable = false)
    private Integer roleLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_role_id")
    private SystemRole systemRole;

    @OneToMany(mappedBy = "clubRole", cascade = CascadeType.ALL)
    private Set<RoleMemberShip> roleMemberships;
}

