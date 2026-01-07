package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "system_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "systemRole", cascade = CascadeType.ALL)
    private Set<User> users;

    @OneToMany(mappedBy = "systemRole", cascade = CascadeType.ALL)
    private Set<ClubRole> clubRoles;
}

