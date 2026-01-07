package com.sep490.backendclubmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "semesters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_name", nullable = false, length = 100)
    private String semesterName;

    @Column(name = "semester_code", unique = true, length = 50)
    private String semesterCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL)
    private Set<RoleMemberShip> roleMemberships;


    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL)
    private Set<Report> reports;
}

