package com.example.event_board.repository;

import com.example.event_board.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndStudentId(Long eventId, Long studentId);
    List<EventRegistration> findByStudentId(Long studentId);
    List<EventRegistration> findByEventId(Long eventId);

    boolean existsByEvent_IdAndStudent_Id(Long eventId, Long studentId);
    List<EventRegistration> findByEvent_Id(Long eventId);
    List<EventRegistration> findByStudent_Id(Long studentId);
}
