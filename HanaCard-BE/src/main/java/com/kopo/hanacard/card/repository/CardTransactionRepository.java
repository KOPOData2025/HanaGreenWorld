package com.kopo.hanacard.card.repository;

import com.kopo.hanacard.card.domain.CardTransaction;
import com.kopo.hanacard.card.domain.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    @Query("SELECT ct FROM CardTransaction ct JOIN FETCH ct.userCard WHERE ct.userCard = :userCard ORDER BY ct.transactionDate DESC")
    List<CardTransaction> findByUserCardOrderByTransactionDateDesc(@Param("userCard") UserCard userCard);

    @Query("SELECT ct FROM CardTransaction ct WHERE ct.userCard = :userCard AND ct.transactionDate BETWEEN :startDate AND :endDate ORDER BY ct.transactionDate DESC")
    List<CardTransaction> findByUserCardAndTransactionDateBetweenOrderByTransactionDateDesc(
            @Param("userCard") UserCard userCard, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ct FROM CardTransaction ct WHERE ct.userCard = :userCard AND ct.transactionDate >= :startDate ORDER BY ct.transactionDate DESC")
    List<CardTransaction> findRecentTransactions(@Param("userCard") UserCard userCard, 
                                               @Param("startDate") LocalDateTime startDate);

    @Query("SELECT ct.category, SUM(ct.amount) as totalAmount FROM CardTransaction ct " +
           "WHERE ct.userCard = :userCard AND ct.transactionDate >= :startDate " +
           "GROUP BY ct.category ORDER BY totalAmount DESC")
    List<Object[]> findMonthlyConsumptionByCategory(@Param("userCard") UserCard userCard, 
                                                   @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(ct.cashbackAmount) FROM CardTransaction ct WHERE ct.userCard = :userCard AND ct.transactionDate >= :startDate")
    Long getTotalCashbackForMonth(@Param("userCard") UserCard userCard, @Param("startDate") LocalDateTime startDate);
    
    List<CardTransaction> findByUserCard(UserCard userCard);
    
    List<CardTransaction> findByUserCardAndTransactionDateBetween(UserCard userCard, LocalDateTime startDate, LocalDateTime endDate);
}
