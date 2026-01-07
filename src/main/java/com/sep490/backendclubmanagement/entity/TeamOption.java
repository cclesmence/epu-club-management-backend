package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.recruitment.Recruitment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
}
