package com.lastdrop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lastdrop.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByChangedAtAsc(Long productId);
}
