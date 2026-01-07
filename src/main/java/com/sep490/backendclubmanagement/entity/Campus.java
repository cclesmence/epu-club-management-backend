package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "campuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campus_name", nullable = false, length = 200)
    private String campusName;

    @Column(name = "campus_code", unique = true, length = 50)
    private String campusCode;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL)
    private Set<Club> clubs;

    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL)
    private Set<AdminDepartment> adminDepartments;
}

