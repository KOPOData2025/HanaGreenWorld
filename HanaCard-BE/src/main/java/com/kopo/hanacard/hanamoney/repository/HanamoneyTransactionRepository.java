package com.kopo.hanacard.hanamoney.repository;

import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.hanamoney.domain.HanamoneyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HanamoneyTransactionRepository extends JpaRepository<HanamoneyTransaction, Long> {
    
    List<HanamoneyTransaction> findByMembership(HanamoneyMembership membership);
    
    List<HanamoneyTransaction> findByMembershipAndCreatedAtBetween(HanamoneyMembership membership, 
                                                                  LocalDateTime startDate, 
                                                                  LocalDateTime endDate);
    
    List<HanamoneyTransaction> findByMembershipAndTransactionType(HanamoneyMembership membership, 
                                                                 HanamoneyTransaction.TransactionType transactionType);
}

