package com.comspare.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartHistoryRepository extends JpaRepository<PartHistory, Long> {

    List<PartHistory> findByPartIdOrderByEventDateDesc(Long partId);
}
