package com.sep490.backendclubmanagement.entity.event;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.RequestEvent;
import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_draft")
    private Boolean isDraft = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = true)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id")
    private EventType eventType;


    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private Set<EventMedia> eventMedia;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private Set<EventAttendance> eventAttendances;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL)
    private RequestEvent requestEvent;


}

