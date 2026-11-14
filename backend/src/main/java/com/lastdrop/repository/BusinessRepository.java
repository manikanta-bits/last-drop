package com.lastdrop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    long countByStatus(BusinessStatus status);

    @Query("select b from Business b join fetch b.owner order by b.createdAt desc")
    List<Business> findAllWithOwner();

    @Query("select b from Business b join fetch b.owner where b.status = :status order by b.createdAt")
    List<Business> findByStatusWithOwner(BusinessStatus status);
}
