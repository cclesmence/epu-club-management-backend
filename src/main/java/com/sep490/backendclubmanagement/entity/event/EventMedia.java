package com.sep490.backendclubmanagement.entity.event;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "media_type", length = 50)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType; // IMAGE, VIDEO, etc.

    @Column(name = "display_order")
    private Integer displayOrder;
}

