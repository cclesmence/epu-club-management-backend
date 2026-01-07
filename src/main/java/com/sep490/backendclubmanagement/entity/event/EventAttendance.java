package com.sep490.backendclubmanagement.entity.event;

import com.sep490.backendclubmanagement.entity.AttendanceStatus;
import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_attendances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registration_time")
    private LocalDateTime registrationTime;

    @Column(name = "attendance_status", length = 50)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus attendanceStatus; // REGISTERED, ATTENDED, ABSENT, CANCELLED

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}

