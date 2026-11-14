package com.lastdrop.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lastdrop.entity.Enums.ReservationStatus;
import com.lastdrop.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
           select r from Reservation r join fetch r.product p join fetch p.business
           where r.buyer.id = :buyerId order by r.createdAt desc
           """)
    List<Reservation> findForBuyer(Long buyerId);

    @Query("""
           select r from Reservation r join fetch r.product p join fetch r.buyer
           where p.business.id = :businessId order by r.createdAt desc
           """)
    List<Reservation> findForBusiness(Long businessId);

    List<Reservation> findByStatusAndPickupByLessThanEqual(ReservationStatus status, Instant now);

    List<Reservation> findByProductIdAndStatus(Long productId, ReservationStatus status);

    long countByStatus(ReservationStatus status);

    long countByBuyerIdAndStatus(Long buyerId, ReservationStatus status);

    long countByProductBusinessIdAndStatus(Long businessId, ReservationStatus status);

    @Query("select coalesce(sum(r.quantity), 0) from Reservation r where r.status = :status")
    long sumQuantity(ReservationStatus status);

    @Query("select coalesce(sum(r.quantity), 0) from Reservation r where r.buyer.id = :buyerId and r.status in :statuses")
    long sumQuantityForBuyer(Long buyerId, Collection<ReservationStatus> statuses);

    @Query("select coalesce(sum(r.quantity), 0) from Reservation r where r.product.business.id = :businessId and r.status = :status")
    long sumQuantityForBusiness(Long businessId, ReservationStatus status);

    @Query("select coalesce(sum(r.totalPrice), 0) from Reservation r where r.product.business.id = :businessId and r.status = :status")
    BigDecimal sumRevenueForBusiness(Long businessId, ReservationStatus status);

    /** Money saved = (normal price - price paid) x quantity. */
    @Query("select coalesce(sum((r.product.originalPrice - r.unitPrice) * r.quantity), 0) from Reservation r where r.status in :statuses")
    BigDecimal sumSavings(Collection<ReservationStatus> statuses);

    @Query("""
           select coalesce(sum((r.product.originalPrice - r.unitPrice) * r.quantity), 0) from Reservation r
           where r.buyer.id = :buyerId and r.status in :statuses
           """)
    BigDecimal sumSavingsForBuyer(Long buyerId, Collection<ReservationStatus> statuses);
}
