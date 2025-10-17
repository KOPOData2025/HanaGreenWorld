package com.kopo.hanacard.hanamoney.repository;

import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HanamoneyMembershipRepository extends JpaRepository<HanamoneyMembership, Long> {
    
    Optional<HanamoneyMembership> findByUser_Id(Long userId);
    
    Optional<HanamoneyMembership> findByMembershipId(String membershipId);
    
    boolean existsByUser_Id(Long userId);
}
