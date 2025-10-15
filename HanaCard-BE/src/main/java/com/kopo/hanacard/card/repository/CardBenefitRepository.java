package com.kopo.hanacard.card.repository;

import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.domain.CardBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardBenefitRepository extends JpaRepository<CardBenefit, Long> {

    List<CardBenefit> findByCardProductAndBenefitTypeAndIsActive(CardProduct cardProduct, String benefitType, Boolean isActive);

    List<CardBenefit> findByCardProductAndIsActiveTrue(CardProduct cardProduct);
}






