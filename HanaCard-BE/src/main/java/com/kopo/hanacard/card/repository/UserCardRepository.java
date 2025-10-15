package com.kopo.hanacard.card.repository;

import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {
    
    List<UserCard> findByUser(User user);
    
    List<UserCard> findByUserAndIsActive(User user, Boolean isActive);
    
    Optional<UserCard> findByCardNumber(String cardNumber);
    
    boolean existsByCardNumber(String cardNumber);
    
    List<UserCard> findByUserAndCardProduct_ProductType(User user, String productType);
    
    List<UserCard> findByUserAndIsActiveTrue(User user);
    
    @Query("SELECT uc FROM UserCard uc JOIN FETCH uc.cardProduct JOIN FETCH uc.user WHERE uc.user.id = :userId AND uc.isActive = :isActive")
    List<UserCard> findByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") Boolean isActive);
    
    List<UserCard> findByUserIdAndIsActiveTrue(Long userId);
}




