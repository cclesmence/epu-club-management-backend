package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "club_creation_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubCreationStep  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String description;
    private Integer orderIndex;
    private Boolean active;

    @OneToMany(mappedBy = "clubCreationStep", cascade = CascadeType.ALL)
    private Set<ClubCreationWorkFlowHistory> clubCreationWorkFlowHistories;
}
