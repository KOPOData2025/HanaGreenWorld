package com.kopo.hanacard.card.service;

import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.dto.CardProductResponse;
import com.kopo.hanacard.card.repository.CardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardProductService {

    private final CardProductRepository cardProductRepository;

    public List<CardProductResponse> getAllActiveCardProducts() {
        List<CardProduct> products = cardProductRepository.findByIsActiveTrue();
        return products.stream()
                .map(CardProductResponse::new)
                .collect(Collectors.toList());
    }

    public List<CardProductResponse> getCardProductsByType(String productType) {
        List<CardProduct> products = cardProductRepository.findByProductTypeAndIsActiveTrue(productType);
        return products.stream()
                .map(CardProductResponse::new)
                .collect(Collectors.toList());
    }
}
