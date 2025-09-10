package com.example.event_board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name="event_registration", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id","student_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name="event_id")
    private Event event;

    @ManyToOne(optional = false) @JoinColumn(name="student_id")
    private User student;

    @Column(name="google_event_id", length = 128)
    private String googleEventId;

}
