package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.dto.CardTransactionResponse;
import com.kopo.hanagreenworld.integration.dto.CardConsumptionSummaryResponse;
import com.kopo.hanagreenworld.integration.dto.CardIntegratedInfoResponse;
import com.kopo.hanagreenworld.merchant.domain.EcoMerchantTransaction;
import com.kopo.hanagreenworld.merchant.repository.EcoMerchantTransactionRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionIntegrationService {

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final GroupIntegrationService groupIntegrationService;
    private final EcoMerchantTransactionRepository ecoMerchantTransactionRepository;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    public List<CardTransactionResponse> getCardTransactions(Long memberId) {
        try {

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/transactions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }
                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 거래내역 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public CardConsumptionSummaryResponse getMonthlyConsumptionSummary(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                Map<String, Integer> categoryAmounts = new HashMap<>();
                if (data.get("categoryAmounts") != null) {
                    Map<String, Object> rawCategoryAmounts = (Map<String, Object>) data.get("categoryAmounts");
                    for (Map.Entry<String, Object> entry : rawCategoryAmounts.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            categoryAmounts.put(entry.getKey(), ((Number) value).intValue());
                        }
                    }
                }
                
                CardConsumptionSummaryResponse result = CardConsumptionSummaryResponse.builder()
                        .totalAmount(data.get("totalAmount") != null ? 
                            Long.valueOf(data.get("totalAmount").toString()) : 0L)
                        .totalCashback(data.get("totalCashback") != null ? 
                            Long.valueOf(data.get("totalCashback").toString()) : 0L)
                        .categoryAmounts(categoryAmounts)
                        .recentTransactions(new ArrayList<>())
                        .build();

                return result;
            } else {
                return CardConsumptionSummaryResponse.builder()
                        .totalAmount(0L)
                        .totalCashback(0L)
                        .categoryAmounts(new HashMap<>())
                        .recentTransactions(new ArrayList<>())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 월간 소비현황 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(0L)
                    .totalCashback(0L)
                    .categoryAmounts(new HashMap<>())
                    .recentTransactions(new ArrayList<>())
                    .build();
        }
    }

    public CardConsumptionSummaryResponse getConsumptionSummary(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
            
            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            headers.set("X-Requesting-Service", "GREEN_WORLD");

            HttpEntity<String> entity = new HttpEntity<>(headers);


            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                CardConsumptionSummaryResponse result = CardConsumptionSummaryResponse.builder()
                        .totalAmount(((Number) data.get("totalAmount")).longValue())
                        .totalCashback(((Number) data.get("totalCashback")).longValue())
                        .categoryAmounts((Map<String, Integer>) data.get("categoryAmounts"))
                        .recentTransactions(new ArrayList<>())
                        .build();

                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return CardConsumptionSummaryResponse.builder()
                        .totalAmount(0L)
                        .totalCashback(0L)
                        .categoryAmounts(new HashMap<>())
                        .recentTransactions(new ArrayList<>())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 월간 소비현황 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(0L)
                    .totalCashback(0L)
                    .categoryAmounts(new HashMap<>())
                    .recentTransactions(new ArrayList<>())
                    .build();
        }
    }

    public List<CardTransactionResponse> getTransactionsByCategory(Long memberId, String category) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/cards/user/" + memberId + "/transactions/category/" + category;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }

                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 카테고리별 거래내역 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public CardIntegratedInfoResponse.CardListInfo getCardList(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId;
            log.info("하나카드 서버 API 호출 준비 - URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> summary = (Map<String, Object>) data.get("summary");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");

                    // 첫 번째 카드를 주 카드로 사용
                    String primaryCardName = "";
                    String primaryCardType = "";
                    if (!cards.isEmpty()) {
                        Map<String, Object> primaryCard = cards.get(0);
                        primaryCardName = (String) primaryCard.get("cardName");
                        primaryCardType = (String) primaryCard.get("cardType");
                    }
                    
                    // 실제 카드 목록 매핑
                    List<CardIntegratedInfoResponse.CardDetail> cardDetails = new ArrayList<>();
                    for (Map<String, Object> cardData : cards) {
                        java.time.LocalDateTime issueDate = null;
                        java.time.LocalDateTime expiryDate = null;
                        
                        try {
                            Object issueDateObj = cardData.get("issueDate");
                            if (issueDateObj instanceof String) {
                                issueDate = java.time.LocalDateTime.parse((String) issueDateObj);
                            } else if (issueDateObj instanceof java.time.LocalDateTime) {
                                issueDate = (java.time.LocalDateTime) issueDateObj;
                            }
                            
                            Object expiryDateObj = cardData.get("expiryDate");
                            if (expiryDateObj instanceof String) {
                                expiryDate = java.time.LocalDateTime.parse((String) expiryDateObj);
                            } else if (expiryDateObj instanceof java.time.LocalDateTime) {
                                expiryDate = (java.time.LocalDateTime) expiryDateObj;
                            }
                        } catch (Exception e) {
                            log.warn("날짜 파싱 실패 - issueDate: {}, expiryDate: {}", cardData.get("issueDate"), cardData.get("expiryDate"));
                        }

                        String imageUrl = (String) cardData.get("cardImageUrl");
                        log.info("카드 데이터 매핑 시작 - 카드명: {}, 이미지URL: {}", cardData.get("cardName"), imageUrl);

                        CardIntegratedInfoResponse.CardDetail cardDetail = CardIntegratedInfoResponse.CardDetail.builder()
                                .cardNumber((String) cardData.get("cardNumber"))
                                .cardName((String) cardData.get("cardName"))
                                .cardType((String) cardData.get("cardType"))
                                .cardStatus((String) cardData.get("cardStatus"))
                                .creditLimit(((Number) cardData.getOrDefault("creditLimit", 0)).longValue())
                                .availableLimit(((Number) cardData.getOrDefault("availableLimit", 0)).longValue())
                                .monthlyUsage(((Number) cardData.getOrDefault("monthlyUsage", 0)).longValue())
                                .cardImageUrl(imageUrl)
                                .issueDate(issueDate)
                                .expiryDate(expiryDate)
                                .benefits((List<String>) cardData.get("benefits"))
                                .build();
                        cardDetails.add(cardDetail);
                    }
                    
                    return CardIntegratedInfoResponse.CardListInfo.builder()
                            .totalCards(((Number) summary.getOrDefault("totalCardCount", 0)).longValue())
                            .totalCreditLimit(((Number) summary.getOrDefault("totalCreditLimit", 0)).longValue())
                            .usedAmount(((Number) summary.getOrDefault("monthlyTotalUsage", 0)).longValue())
                            .availableLimit(((Number) summary.getOrDefault("totalAvailableLimit", 0)).longValue())
                            .primaryCardName(primaryCardName)
                            .primaryCardType(primaryCardType)
                            .cards(cardDetails)
                            .build();
                }
            }
            
            log.warn("하나카드 서버에서 카드 목록 조회 실패 또는 빈 응답 - 회원ID: {}", memberId);
            
        } catch (Exception e) {
            log.error("하나카드 서버 카드 목록 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
        }
        
        // 실패 시 빈 카드 정보 반환
        return CardIntegratedInfoResponse.CardListInfo.builder()
                .totalCards(0L)
                .totalCreditLimit(0L)
                .usedAmount(0L)
                .availableLimit(0L)
                .primaryCardName("")
                .primaryCardType("")
                .cards(new ArrayList<>()) // 빈 카드 목록
                .build();
    }

    public CardIntegratedInfoResponse getCardIntegratedInfo(Long memberId) {
        try {
            log.info("카드 통합 정보 조회 시작 - 회원ID: {}", memberId);

            // 카드 거래내역 조회
            List<CardTransactionResponse> transactions = getCardTransactions(memberId);
            
            // 친환경 소비현황 조회 (card_transactions에서 친환경 태그 필터링)
            CardConsumptionSummaryResponse consumptionSummary = getEcoConsumptionSummaryFromCardTransactions(memberId);
            
            // 친환경 혜택 정보 조회 (eco_merchant_transactions에서 이번달만)
            Map<String, Object> ecoBenefits = getEcoBenefitsFromEcoMerchantTransactions(memberId);
            
            // 카드 목록 조회 (하나카드 서버에서)
            CardIntegratedInfoResponse.CardListInfo cardList = getCardList(memberId);

            CardIntegratedInfoResponse response = CardIntegratedInfoResponse.builder()
                    .cardList(cardList)
                    .transactions(transactions)
                    .consumptionSummary(consumptionSummary)
                    .ecoBenefits(ecoBenefits)
                    .build();

            return response;

        } catch (Exception e) {
            log.error("카드 통합 정보 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardIntegratedInfoResponse.builder()
                    .cardList(CardIntegratedInfoResponse.CardListInfo.builder()
                            .totalCards(0L)
                            .totalCreditLimit(0L)
                            .usedAmount(0L)
                            .availableLimit(0L)
                            .primaryCardName("")
                            .primaryCardType("")
                            .build())
                    .transactions(new ArrayList<>())
                    .consumptionSummary(CardConsumptionSummaryResponse.builder()
                            .totalAmount(0L)
                            .totalCashback(0L)
                            .categoryAmounts(new HashMap<>())
                            .build())
                    .ecoBenefits(getDefaultEcoBenefits())
                    .build();
        }
    }

    private Map<String, Object> getDefaultEcoBenefits() {
        Map<String, Object> benefits = new HashMap<>();
        benefits.put("totalBenefits", 2500L);
        benefits.put("benefits", new ArrayList<>());
        benefits.put("stats", Map.of(
            "totalEcoTransactions", 0,
            "totalEcoAmount", 0L,
            "totalAdditionalSeeds", 0L,
            "averageAdditionalSeeds", 0,
            "ecoMerchantCount", 0
        ));
        return benefits;
    }


    public CardConsumptionSummaryResponse getEcoConsumptionSummaryFromCardTransactions(Long memberId) {
        try {
            log.info("친환경 소비현황 조회 시작 (card_transactions 기반, 이번달만) - 회원ID: {}", memberId);
            
            // 모든 카드 거래내역 조회
            List<CardTransactionResponse> allTransactions = getCardTransactions(memberId);
            
            // 이번달 거래만 필터링
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
            
            // 친환경 태그가 붙은 거래만 필터링 (이번달만)
            List<CardTransactionResponse> ecoTransactions = allTransactions.stream()
                    .filter(transaction -> {
                        // 이번달 거래인지 확인
                        LocalDate transactionDate = LocalDate.parse(transaction.getTransactionDate().substring(0, 10));
                        boolean isCurrentMonth = !transactionDate.isBefore(startOfMonth) && !transactionDate.isAfter(endOfMonth);
                        
                        // 친환경 거래인지 확인
                        boolean isEco = isEcoTransaction(transaction);
                        
                        return isCurrentMonth && isEco;
                    })
                    .collect(Collectors.toList());
            
            if (ecoTransactions.isEmpty()) {
                log.info("친환경 태그가 붙은 거래내역이 없습니다 - 회원ID: {}", memberId);
                return CardConsumptionSummaryResponse.builder()
                        .totalAmount(0L)
                        .totalCashback(0L)
                        .categoryAmounts(new HashMap<>())
                        .recentTransactions(new ArrayList<>())
                        .build();
            }
            
            // 총 소비금액 계산
            Long totalAmount = ecoTransactions.stream()
                    .mapToLong(CardTransactionResponse::getAmount)
                    .sum();
            
            // 총 캐시백 계산
            Long totalCashback = ecoTransactions.stream()
                    .mapToLong(CardTransactionResponse::getCashbackAmount)
                    .sum();
            
            // 카테고리별 소비금액 계산
            Map<String, Long> categoryAmounts = ecoTransactions.stream()
                    .collect(Collectors.groupingBy(
                            CardTransactionResponse::getCategory,
                            Collectors.summingLong(CardTransactionResponse::getAmount)
                    ));
            
            // 최근 거래내역 (최대 10건)
            List<CardTransactionResponse> recentTransactions = ecoTransactions.stream()
                    .limit(10)
                    .collect(Collectors.toList());
            
            log.info("친환경 소비현황 조회 완료 - 회원ID: {}, 총소비: {}, 총캐시백: {}, 거래건수: {}", 
                    memberId, totalAmount, totalCashback, ecoTransactions.size());
            
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(totalAmount)
                    .totalCashback(totalCashback)
                    .categoryAmounts(categoryAmounts.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().intValue()
                            )))
                    .recentTransactions(recentTransactions)
                    .build();
                    
        } catch (Exception e) {
            log.error("친환경 소비현황 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(0L)
                    .totalCashback(0L)
                    .categoryAmounts(new HashMap<>())
                    .recentTransactions(new ArrayList<>())
                    .build();
        }
    }

    public Map<String, Object> getEcoBenefitsFromEcoMerchantTransactions(Long memberId) {
        try {
            log.info("친환경 혜택 정보 조회 시작 (eco_merchant_transactions 기반, 이번달만) - 회원ID: {}", memberId);
            
            // 이번달 친환경 가맹점 거래내역 조회
            List<EcoMerchantTransaction> ecoTransactions = ecoMerchantTransactionRepository.findCurrentMonthTransactionsByMemberId(memberId);
            
            if (ecoTransactions.isEmpty()) {
                return getDefaultEcoBenefits();
            }
            
            // 총 원큐씨앗 계산
            Long totalSeeds = ecoTransactions.stream()
                    .mapToLong(EcoMerchantTransaction::getEarnedSeeds)
                    .sum();
            
            // 총 친환경 소비금액 계산
            Long totalEcoAmount = ecoTransactions.stream()
                    .mapToLong(EcoMerchantTransaction::getTransactionAmount)
                    .sum();
            
            // 평균 원큐씨앗 계산
            int averageSeeds = totalSeeds.intValue() / ecoTransactions.size();
            
            // 친환경 가맹점 수 계산 (중복 제거)
            long ecoMerchantCount = ecoTransactions.stream()
                    .map(EcoMerchantTransaction::getMerchantName)
                    .distinct()
                    .count();
            
            // 혜택 목록 생성
            List<Map<String, Object>> benefits = ecoTransactions.stream()
                    .map(transaction -> {
                        Map<String, Object> benefit = new HashMap<>();
                        benefit.put("storeName", transaction.getMerchantName());
                        benefit.put("category", transaction.getMerchantCategory());
                        benefit.put("amount", transaction.getEarnedSeeds());
                        benefit.put("transactionAmount", transaction.getTransactionAmount());
                        benefit.put("transactionDate", transaction.getTransactionDate());
                        return benefit;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalBenefits", totalSeeds);
            result.put("benefits", benefits);
            result.put("stats", Map.of(
                "totalEcoTransactions", ecoTransactions.size(),
                "totalEcoAmount", totalEcoAmount,
                "totalAdditionalSeeds", totalSeeds,
                "averageAdditionalSeeds", averageSeeds,
                "ecoMerchantCount", ecoMerchantCount
            ));
            
            log.info("친환경 혜택 정보 조회 완료 - 회원ID: {}, 총원큐씨앗: {}, 거래건수: {}", 
                    memberId, totalSeeds, ecoTransactions.size());
            
            return result;
                    
        } catch (Exception e) {
            log.error("친환경 혜택 정보 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return getDefaultEcoBenefits();
        }
    }
    
    /**
     * 거래가 친환경 거래인지 판단하는 메서드 (카테고리 기반만)
     */
    private boolean isEcoTransaction(CardTransactionResponse transaction) {
        // 친환경 관련 카테고리 목록
        List<String> ecoCategories = Arrays.asList(
            "유기농식품", "공유킥보드", "전기차", "친환경브랜드", 
            "중고거래", "리필샵", "대중교통", "친환경", "재활용", 
            "제로웨이스트", "친환경뷰티", "친환경쇼핑", "유기농카페",
            "ECO_FOOD", "ECO_SHOPPING", "ECO_TRANSPORT", "ECO_BEAUTY",
            "ECO_LIFESTYLE", "ECO_CAFE", "ECO_MARKET"
        );
        
        // 카테고리로만 판단
        return ecoCategories.contains(transaction.getCategory());
    }

    private String generateMockCI(Member member) {
        return "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
    }
}
