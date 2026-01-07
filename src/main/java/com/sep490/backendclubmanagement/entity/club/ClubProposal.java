package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.RequestEstablishment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "club_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubProposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_establishment_id")
    private RequestEstablishment requestEstablishment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;
}

