package com.example.event_board.repository;

import com.example.event_board.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"company", "createdBy"})
    Page<Event> findByStartTimeAfter(LocalDateTime now, Pageable pageable);

    List<Event> findByCompanyId(Long companyId);
}
