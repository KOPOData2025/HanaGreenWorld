package com.kopo.hanacard.hanamoney.repository;

import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HanamoneyAccountRepository extends JpaRepository<HanamoneyMembership, Long> {
    
    Optional<HanamoneyMembership> findByUser(User user);
    
    Optional<HanamoneyMembership> findByMembershipId(String membershipId);
}
