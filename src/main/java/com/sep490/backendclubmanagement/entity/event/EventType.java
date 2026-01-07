package com.sep490.backendclubmanagement.entity.event;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "event_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    private String typeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "eventType", cascade = CascadeType.ALL)
    private Set<Event> events;
}

