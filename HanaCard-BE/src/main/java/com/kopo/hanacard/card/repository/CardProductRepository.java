package com.kopo.hanacard.card.repository;

import com.kopo.hanacard.card.domain.CardProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardProductRepository extends JpaRepository<CardProduct, Long> {
    
    List<CardProduct> findByIsActiveTrue();
    
    List<CardProduct> findByProductTypeAndIsActiveTrue(String productType);
    
    @Query("SELECT cp FROM CardProduct cp WHERE cp.isActive = true AND " +
           "(cp.productName LIKE %:keyword% OR cp.description LIKE %:keyword%)")
    List<CardProduct> findByKeywordAndIsActiveTrue(@Param("keyword") String keyword);
}
