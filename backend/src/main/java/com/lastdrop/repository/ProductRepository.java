package com.lastdrop.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Live deals: active, in stock, not expired, from an approved shop. */
    @Query("""
           select p from Product p join fetch p.business b
           where p.status = :active and p.quantityAvailable > 0 and p.expiresAt > :now and b.status = :approved
           """)
    List<Product> findLive(Instant now, ProductStatus active, BusinessStatus approved);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByStatusInAndExpiresAtLessThanEqual(Collection<ProductStatus> statuses, Instant now);

    /** Locks the row until the transaction ends, so two buyers can't take the last item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @Query("select p from Product p join fetch p.business where p.id = :id")
    Optional<Product> findWithBusiness(Long id);

    List<Product> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    @Query("select p from Product p join fetch p.business order by p.createdAt desc")
    List<Product> findAllWithBusiness();

    long countByStatus(ProductStatus status);

    long countByBusinessIdAndStatus(Long businessId, ProductStatus status);

    @Query("select coalesce(sum(p.quantityTotal), 0) from Product p where p.business.id = :businessId")
    long sumQuantityByBusiness(Long businessId);
}
