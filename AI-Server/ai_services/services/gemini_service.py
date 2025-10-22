"""
Gemini API 서비스 - 다층 검증 시스템
"""
import google.generativeai as genai
from django.conf import settings
import logging
import json
from typing import Dict, Any, Optional, List, Tuple
import torch
from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import io
import re

logger = logging.getLogger(__name__)

class GeminiService:
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다.")
        
        genai.configure(api_key=self.api_key)
        self.model = genai.GenerativeModel('gemini-2.0-flash')
        self.vision_model = genai.GenerativeModel('gemini-2.0-flash')
        
        # CLIP 모델 초기화 (지연 로딩)
        self._clip_model = None
        self._clip_processor = None
        
        # 챌린지별 키워드 매핑 정의
        self.challenge_keywords = {
            'TUMBLER_CHALLENGE': {
                'ko': ['텀블러', '물병', '보온병', '다회용', '컵', '병', '사용', '들고', '마시', '활용'],
                'en': ['tumbler', 'water bottle', 'reusable bottle', 'thermos', 'cup', 'bottle', 'using', 'holding', 'drinking'],
                'clip_threshold': 0.5,
                'keyword_threshold': 1,
                'forbidden_keywords': ['일회용', '플라스틱', '스티로폼', '종이컵', 'disposable', 'plastic', 'styrofoam', 'paper cup']  # 금지 키워드 (CLIP이 혼동할 수 있는 것들만)
            },
            'REUSABLE_BAG': {
                'ko': ['장바구니', '에코백', '가방', '쇼핑백', '재사용'],
                'en': ['shopping bag', 'eco bag', 'reusable bag', 'tote bag', 'bag'],
                'clip_threshold': 0.5,
                'keyword_threshold': 1,
                'forbidden_keywords': ['일회용', '비닐', '플라스틱', 'disposable', 'plastic', 'vinyl']
            },
            'REUSABLE_BAG_EXTENDED': {
                'ko': ['친환경', '장바구니', '에코백', '가방', '쇼핑백'],
                'en': ['eco-friendly', 'shopping bag', 'eco bag', 'reusable bag', 'tote bag'],
                'clip_threshold': 0.5,
                'keyword_threshold': 1,
                'forbidden_keywords': ['일회용', '비닐', '플라스틱', 'disposable', 'plastic', 'vinyl']
            },
            'PLUGGING': {
                'ko': ['쓰레기', '줍', '집게', '봉투', '플로깅', '줍깅', '정리'],
                'en': ['trash', 'litter', 'picking up', 'plogging', 'cleaning', 'garbage bag'],
                'clip_threshold': 0.4,
                'keyword_threshold': 1,
                'forbidden_keywords': ['실내', '집안', 'indoor', 'home', '실내활동']
            },
            'NO_PLASTIC': {
                'ko': ['일회용', '플라스틱', '다회용', '재사용', '용기', '도구'],
                'en': ['single-use', 'plastic', 'reusable', 'container', 'utensil', 'tool'],
                'clip_threshold': 0.45,
                'keyword_threshold': 1,
                'forbidden_keywords': ['플라스틱', '일회용', 'plastic', 'disposable', '스티로폼']
            },
            'RECYCLE': {
                'ko': ['분리수거', '재활용', '쓰레기', '통', '정리', '분류'],
                'en': ['recycling', 'recycle', 'waste', 'bin', 'sorting', 'separation'],
                'clip_threshold': 0.45,
                'keyword_threshold': 1,
                'forbidden_keywords': ['일반쓰레기', '소각', 'landfill', 'incineration']
            }
        }
    
    def _get_clip_model(self):
        """CLIP 모델 지연 로딩"""
        if self._clip_model is None:
            try:
                logger.info("CLIP 모델 로딩 시작...")
                self._clip_model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
                self._clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
                logger.info("CLIP 모델 로딩 완료")
            except Exception as e:
                logger.error(f"CLIP 모델 로딩 실패: {str(e)}")
                raise
        return self._clip_model, self._clip_processor
    
    def calculate_clip_similarity(self, image_data: bytes, challenge_code: str) -> Dict[str, Any]:
        """
        CLIP을 사용한 이미지-텍스트 유사도 계산
        
        Args:
            image_data: 이미지 바이너리 데이터
            challenge_code: 챌린지 코드
            
        Returns:
            Dict with similarity score and details
        """
        try:
            # CLIP 모델 로드
            clip_model, clip_processor = self._get_clip_model()
            
            # 이미지 변환
            image = Image.open(io.BytesIO(image_data))
            
            # 챌린지별 키워드 가져오기
            challenge_info = self.challenge_keywords.get(challenge_code, {
                'en': ['challenge', 'activity'],
                'clip_threshold': 0.5
            })
            
            # 영문 키워드로 CLIP 검증
            text_queries = challenge_info['en']
            
            # CLIP 처리
            inputs = clip_processor(
                text=text_queries, 
                images=image, 
                return_tensors="pt", 
                padding=True
            )
            
            with torch.no_grad():
                outputs = clip_model(**inputs)
                # 코사인 유사도 계산
                logits_per_image = outputs.logits_per_image
                probs = logits_per_image.softmax(dim=1)
                
                # 최대 유사도 점수
                max_similarity = float(probs.max())
                avg_similarity = float(probs.mean())
                
                # 가장 유사한 키워드
                best_match_idx = probs.argmax().item()
                best_keyword = text_queries[best_match_idx]
                
                logger.info(f"CLIP 유사도 - 최대: {max_similarity:.3f}, 평균: {avg_similarity:.3f}, 최적 키워드: {best_keyword}")
                
                return {
                    'success': True,
                    'max_similarity': max_similarity,
                    'avg_similarity': avg_similarity,
                    'best_keyword': best_keyword,
                    'threshold': challenge_info['clip_threshold'],
                    'passed': max_similarity >= challenge_info['clip_threshold']
                }
                
        except Exception as e:
            logger.error(f"CLIP 유사도 계산 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'max_similarity': 0.0,
                'avg_similarity': 0.0,
                'best_keyword': '',
                'threshold': 0.5,
                'passed': False
            }
    
    def gemini_self_check(self, image_data: bytes, challenge_title: str, challenge_code: str) -> Dict[str, Any]:
        """
        Gemini Self-Check: 긍정형/부정형 질문으로 교차 검증
        
        Args:
            image_data: 이미지 바이너리 데이터
            challenge_title: 챌린지 제목
            challenge_code: 챌린지 코드
            
        Returns:
            Dict with consistency analysis and scores
        """
        try:
            from PIL import Image
            import io
            
            # 이미지 변환
            image = Image.open(io.BytesIO(image_data))
            
            # 1차 질문: 긍정형
            positive_prompt = f"""
당신은 에코 챌린지 사진 인증 전문가입니다. 
제출된 사진이 "{challenge_title}" 챌린지의 인증 사진으로 적합한지 판단해주세요.

**중요한 판단 기준**:
1. 챌린지 목표물이 명확하게 보이는가?
2. 실제 사용/활동 모습이 담겨있는가? (단순 제품 사진은 부적합)
3. 사람이 실제로 챌린지를 수행하는 모습인가?
4. 인터넷에서 다운받은 제품 사진이 아닌가?

**절대 금지 사항** (이 중 하나라도 해당하면 즉시 REJECTED):
- 일회용 컵, 플라스틱 컵, 스티로폼 컵, 종이컵 사용
- 장바구니나 가방을 텀블러로 착각
- 챌린지와 전혀 관련 없는 물건

**신뢰도 기준**:
- 0.9 이상: 완벽한 인증 사진 (목표물 + 실제 사용 모습)
- 0.7-0.9: 좋은 인증 사진 (목표물 명확, 약간의 애매함)
- 0.5-0.7: 애매한 사진 (목표물 보이지만 실제 사용 모습 불명확)
- 0.5 미만: 부적합 (단순 제품 사진, 관련 없음)

반드시 다음 JSON 형식으로만 응답해주세요:
{{
    "verification_result": "APPROVED" | "NEEDS_REVIEW" | "REJECTED",
    "confidence": 0.0-1.0,
    "explanation": "판단 근거에 대한 한글 설명 (2-3문장)",
    "reason": "구체적인 판단 이유 (키워드 포함)"
}}
"""
            
            # 2차 질문: 부정형
            negative_prompt = f"""
당신은 에코 챌린지 사진 인증 전문가입니다.
제출된 사진이 "{challenge_title}" 챌린지와 관련이 없을 가능성을 평가해주세요.

**관련 없을 가능성 기준**:
- 챌린지와 전혀 관련 없는 사진인가?
- 목표물이 전혀 보이지 않는가?
- 가짜나 조작된 사진인가?
- 인터넷에서 다운받은 사진인가?

반드시 다음 JSON 형식으로만 응답해주세요:
{{
    "unrelated_probability": 0.0-1.0,
    "confidence": 0.0-1.0,
    "explanation": "관련 없을 가능성에 대한 한글 설명",
    "reason": "구체적인 판단 이유"
}}
"""
            
            # 1차 검증 (긍정형)
            logger.info("Gemini 1차 검증 (긍정형) 시작...")
            positive_response = self.vision_model.generate_content([positive_prompt, image])
            positive_text = positive_response.text.strip()
            
            # 2차 검증 (부정형)
            logger.info("Gemini 2차 검증 (부정형) 시작...")
            negative_response = self.vision_model.generate_content([negative_prompt, image])
            negative_text = negative_response.text.strip()
            
            # JSON 파싱
            positive_result = self._parse_gemini_response(positive_text)
            negative_result = self._parse_gemini_response(negative_text)
            
            # 상세 로그 출력
            logger.info(f"🔍 긍정 질문 원본 응답: {positive_text}")
            logger.info(f"🔍 부정 질문 원본 응답: {negative_text}")
            logger.info(f"📊 긍정 결과 파싱: {positive_result}")
            logger.info(f"📊 부정 결과 파싱: {negative_result}")
            
            # 일관성 검증
            consistency_score = self._calculate_consistency(positive_result, negative_result)
            
            logger.info(f"Self-check 결과 - 긍정 신뢰도: {positive_result.get('confidence', 0):.3f}, "
                       f"부정 신뢰도: {negative_result.get('confidence', 0):.3f}, "
                       f"unrelated_probability: {negative_result.get('unrelated_probability', 0):.3f}, "
                       f"일관성: {consistency_score:.3f}")
            
            return {
                'success': True,
                'positive_result': positive_result,
                'negative_result': negative_result,
                'consistency_score': consistency_score,
                'is_consistent': consistency_score >= 0.7,
                'final_confidence': (positive_result.get('confidence', 0) + 
                                   (1 - negative_result.get('unrelated_probability', 0))) / 2
            }
            
        except Exception as e:
            logger.error(f"Gemini Self-Check 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'consistency_score': 0.0,
                'is_consistent': False,
                'final_confidence': 0.0
            }
    
    def _parse_gemini_response(self, response_text: str) -> Dict[str, Any]:
        """Gemini 응답 JSON 파싱"""
        try:
            # 마크다운 코드 블록 제거
            text = response_text.strip()
            if text.startswith('```json'):
                text = text[7:]
            if text.endswith('```'):
                text = text[:-3]
            text = text.strip()
            
            return json.loads(text)
        except json.JSONDecodeError as e:
            logger.error(f"JSON 파싱 실패: {e}, 원본: {response_text}")
            return {
                'verification_result': 'NEEDS_REVIEW',
                'confidence': 0.5,
                'explanation': 'AI 응답 파싱 실패',
                'reason': '파싱 오류'
            }
    
    def _calculate_consistency(self, positive_result: Dict, negative_result: Dict) -> float:
        """일관성 점수 계산"""
        try:
            positive_confidence = positive_result.get('confidence', 0)
            unrelated_prob = negative_result.get('unrelated_probability', 0)
            
            # 일관성 계산: 긍정 신뢰도와 (1 - 부정 확률)의 차이
            expected_negative = 1 - positive_confidence
            actual_negative = unrelated_prob
            
            # 차이가 작을수록 일관성 높음
            consistency = 1 - abs(expected_negative - actual_negative)
            
            # 상세 로그 출력
            logger.info(f"🧮 일관성 계산 상세:")
            logger.info(f"   - positive_confidence: {positive_confidence:.3f}")
            logger.info(f"   - unrelated_prob: {unrelated_prob:.3f}")
            logger.info(f"   - expected_negative (1 - positive): {expected_negative:.3f}")
            logger.info(f"   - actual_negative (unrelated_prob): {actual_negative:.3f}")
            logger.info(f"   - 차이: {abs(expected_negative - actual_negative):.3f}")
            logger.info(f"   - 일관성: {consistency:.3f}")
            
            return max(0.0, min(1.0, consistency))
        except Exception as e:
            logger.error(f"일관성 계산 오류: {e}")
            return 0.0
    
    def validate_keywords(self, reason_text: str, challenge_code: str) -> Dict[str, Any]:
        """
        근거 설명(reason)에서 키워드 검증 (금지 키워드 포함)
        
        Args:
            reason_text: Gemini가 제공한 판단 근거
            challenge_code: 챌린지 코드
            
        Returns:
            Dict with keyword validation results
        """
        try:
            # 챌린지별 키워드 가져오기
            challenge_info = self.challenge_keywords.get(challenge_code, {
                'ko': ['챌린지', '활동'],
                'keyword_threshold': 1,
                'forbidden_keywords': []
            })
            
            required_keywords = challenge_info['ko']
            threshold = challenge_info['keyword_threshold']
            forbidden_keywords = challenge_info.get('forbidden_keywords', [])
            
            # 키워드 매칭 (대소문자 무시)
            reason_lower = reason_text.lower()
            matched_keywords = []
            matched_forbidden = []
            
            # 필수 키워드 검증
            for keyword in required_keywords:
                if keyword.lower() in reason_lower:
                    matched_keywords.append(keyword)
            
            # 금지 키워드 검증
            for keyword in forbidden_keywords:
                if keyword.lower() in reason_lower:
                    matched_forbidden.append(keyword)
            
            # 매칭 점수 계산
            match_count = len(matched_keywords)
            keyword_score = min(1.0, match_count / threshold)
            
            # 키워드가 없으면 감점
            penalty = 0.0 if match_count >= threshold else 0.3
            
            # 금지 키워드가 있으면 강한 감점
            if matched_forbidden:
                penalty += 0.5  # 금지 키워드 발견 시 추가 감점
                logger.warning(f"금지 키워드 발견: {matched_forbidden}")
            
            # 금지 키워드가 있으면 자동 실패
            passed = match_count >= threshold and len(matched_forbidden) == 0
            
            logger.info(f"키워드 검증 - 필수 매칭: {matched_keywords}, 금지 매칭: {matched_forbidden}, "
                       f"점수: {keyword_score:.3f}, 감점: {penalty:.3f}, 통과: {passed}")
            
            return {
                'success': True,
                'matched_keywords': matched_keywords,
                'matched_forbidden': matched_forbidden,
                'match_count': match_count,
                'required_count': threshold,
                'keyword_score': keyword_score,
                'penalty': penalty,
                'passed': passed,
                'has_forbidden': len(matched_forbidden) > 0
            }
            
        except Exception as e:
            logger.error(f"키워드 검증 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'matched_keywords': [],
                'matched_forbidden': [],
                'match_count': 0,
                'required_count': 1,
                'keyword_score': 0.0,
                'penalty': 0.2,
                'passed': False,
                'has_forbidden': False
            }
    
    def generate_text(self, prompt: str, **kwargs) -> Dict[str, Any]:
        """
        텍스트 생성
        """
        try:
            response = self.model.generate_content(prompt)
            return {
                'success': True,
                'text': response.text,
                'model': 'gemini-2.0-flash'
            }
        except Exception as e:
            logger.error(f"Gemini 텍스트 생성 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gemini-2.0-flash'
            }
    
    def analyze_image(self, image_data: bytes, prompt: str = "이 이미지를 분석해주세요.") -> Dict[str, Any]:
        """
        이미지 분석
        """
        try:
            from PIL import Image
            import io
            
            # 이미지 데이터를 PIL Image로 변환
            image = Image.open(io.BytesIO(image_data))
            
            response = self.vision_model.generate_content([prompt, image])
            return {
                'success': True,
                'text': response.text,
                'model': 'gemini-2.0-flash'
            }
        except Exception as e:
            logger.error(f"Gemini 이미지 분석 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gemini-2.0-flash'
            }
    
    def generate_quiz_question(self) -> Dict[str, Any]:
        """
        퀴즈 문제 생성
        """
        try:
            prompt = f"""
            당신은 환경보호와 녹색금융 전문가입니다. 교육적이고 실용적인 퀴즈를 생성해주세요.
            
            반드시 다음 JSON 형식으로만 응답해주세요:
            {{
                "question": "문제 내용",
                "options": ["선택지1", "선택지2", "선택지3", "선택지4"],
                "correct_answer": 0,
                "explanation": "정답에 대한 자세한 설명"
            }}
            
            주제 선택 가이드라인:
            환경 관련 (1-6번 중 랜덤 선택):
            1. 기후변화와 지구온난화 - 온실가스, 지구온도 상승, 기후변화 영향
            2. 재활용과 자원순환 - 분리수거, 업사이클링, 순환경제
            3. 친환경 생활습관 - 에너지 절약, 물 절약, 친환경 제품 사용
            4. 탄소중립과 신재생에너지 - 태양광, 풍력, 탄소중립 정책
            5. 생물다양성과 생태계 보호 - 멸종위기종, 생태계 복원, 서식지 보호
            6. 대기질과 수질 관리 - 미세먼지, 수질오염, 환경기준
            
            녹색금융 관련 (7-14번 중 랜덤 선택):
            7. ESG 투자와 지속가능 금융 - ESG 평가, 지속가능 투자 상품
            8. 그린본드와 친환경 금융상품 - 그린본드, 친환경 펀드, ESG 펀드
            9. 탄소배출권 거래와 탄소가격제 - K-ETS, 탄소세, 배출권 거래
            10. 친환경 기업 투자와 평가 - 그린기업 인증, 친환경 기업 평가
            11. 지속가능한 금융정책과 규제 - 그린뉴딜, 탄소중립 정책
            12. 기후 리스크와 금융 안정성 - 물리적 리스크, 전환 리스크
            13. 친환경 신용카드와 금융혜택 - 그린카드, 친환경 혜택, 포인트 적립
            14. 그린뱅킹과 디지털 금융 - 디지털 금융, 그린뱅킹 서비스
            
            퀴즈 작성 원칙:
            1. 문제는 명확하고 이해하기 쉬워야 함
            2. 선택지는 모두 그럴듯해야 함 (너무 쉬우거나 어렵지 않게)
            3. 오답 선택지는 교육적 가치가 있어야 함
            4. 해설은 구체적이고 실용적인 정보를 포함해야 함
            5. 한국의 정책이나 사례를 우선적으로 언급
            
            예시 (환경 관련):
            {{
                "question": "다음 중 탄소 발자국을 가장 많이 줄일 수 있는 교통수단은?",
                "options": ["대중교통 이용", "전기차 운전", "하이브리드차 운전", "경유차 운전"],
                "correct_answer": 0,
                "explanation": "대중교통 이용은 개인 자동차 대비 탄소배출량을 50-70% 줄일 수 있어 가장 효과적입니다. 전기차도 좋지만 전력 생산 과정에서의 탄소배출을 고려하면 대중교통이 더 친환경적입니다."
            }}
            
            예시 (녹색금융 관련):
            {{
                "question": "ESG 투자에서 'G'는 무엇을 의미하나요?",
                "options": ["Green(녹색)", "Governance(지배구조)", "Growth(성장)", "Global(글로벌)"],
                "correct_answer": 1,
                "explanation": "ESG는 Environment(환경), Social(사회), Governance(지배구조)의 약자입니다. G는 기업의 지배구조, 투명성, 윤리경영 등을 평가하는 기준입니다."
            }}
            
            이제 위의 가이드라인에 따라 새로운 퀴즈를 생성해주세요.
            """
            
            response = self.model.generate_content(prompt)
            
            # JSON 파싱 시도
            try:
                # 마크다운 코드 블록 제거
                text = response.text.strip()
                if text.startswith('```json'):
                    text = text[7:]  # ```json 제거
                if text.endswith('```'):
                    text = text[:-3]  # ``` 제거
                text = text.strip()
                
                result = json.loads(text)
                return {
                    'success': True,
                    'quiz': result,
                    'model': 'gemini-2.0-flash'
                }
            except json.JSONDecodeError as e:
                logger.error(f"JSON 파싱 실패: {e}, 원본 텍스트: {response.text}")
                # JSON 파싱 실패 시 텍스트 그대로 반환
                return {
                    'success': True,
                    'quiz_text': response.text,
                    'model': 'gemini-2.0-flash'
                }
                
        except Exception as e:
            logger.error(f"Gemini 퀴즈 생성 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gemini-2.0-flash'
            }

    def verify_challenge_image_enhanced(self, image_data: bytes, challenge_title: str, challenge_code: str) -> Dict[str, Any]:
        """
        다층 검증 시스템: CLIP + Gemini Self-Check + 키워드 검증
        
        Args:
            image_data: 이미지 바이너리 데이터
            challenge_title: 챌린지 제목
            challenge_code: 챌린지 코드
        
        Returns:
            Dict with enhanced verification result
        """
        try:
            logger.info(f"🔍 다층 AI 검증 시작 - 챌린지: {challenge_title} ({challenge_code})")
            logger.info(f"📊 이미지 크기: {len(image_data)} bytes")
            
            # 1. CLIP 유사도 검증 (우선 필터링)
            logger.info("1단계: CLIP 유사도 검증...")
            clip_result = self.calculate_clip_similarity(image_data, challenge_code)
            
            # CLIP이 명확히 실패하면 즉시 거부 (임계값보다 낮으면)
            clip_threshold = self.challenge_keywords.get(challenge_code, {}).get('clip_threshold', 0.5)
            if not clip_result.get('passed', False) and clip_result.get('max_similarity', 0) < clip_threshold:
                logger.warning(f"CLIP 우선 필터링: 유사도 {clip_result.get('max_similarity', 0):.3f} < {clip_threshold}, 즉시 거부")
                return {
                    'success': True,
                    'verification_result': 'REJECTED',
                    'confidence': clip_result.get('max_similarity', 0),
                    'explanation': f"CLIP 객체 인식: {challenge_code}와 관련된 객체가 명확하지 않습니다. (유사도: {clip_result.get('max_similarity', 0):.3f})",
                    'detected_items': [],
                    'verification_details': {
                        'gemini_confidence': 0,
                        'clip_similarity': clip_result.get('max_similarity', 0),
                        'consistency_score': 0,
                        'keyword_match': False,
                        'method': 'clip_priority_filter',
                        'clip_passed': False,
                        'self_check_consistent': False,
                        'keyword_penalty': 0
                    },
                    'model': 'enhanced_multi_layer'
                }
            
            # 2. Gemini Self-Check 검증
            logger.info("2단계: Gemini Self-Check 검증...")
            self_check_result = self.gemini_self_check(image_data, challenge_title, challenge_code)
            
            # 3. 키워드 검증 (Self-check의 reason 사용)
            logger.info("3단계: 키워드 검증...")
            positive_reason = self_check_result.get('positive_result', {}).get('reason', '')
            keyword_result = self.validate_keywords(positive_reason, challenge_code)
            
            # 금지 키워드 발견 시 즉시 거절
            if keyword_result.get('has_forbidden', False):
                forbidden_keywords = keyword_result.get('matched_forbidden', [])
                logger.warning(f"🚫 금지 키워드 발견! 챌린지: {challenge_code}, 금지 키워드: {', '.join(forbidden_keywords)}. 즉시 REJECTED 처리.")
                return {
                    'success': True,
                    'verification_result': 'REJECTED',
                    'confidence': 0.1,  # 매우 낮은 신뢰도
                    'explanation': f"금지 키워드 감지: '{', '.join(forbidden_keywords)}'가 설명에서 발견되었습니다. 이는 {challenge_title} 챌린지 규칙에 위배됩니다.",
                    'detected_items': [],
                    'verification_details': {
                        'gemini_confidence': self_check_result.get('final_confidence', 0),
                        'clip_similarity': clip_result.get('max_similarity', 0),
                        'consistency_score': self_check_result.get('consistency_score', 0),
                        'keyword_match': False,
                        'method': 'forbidden_keyword_rejection',
                        'clip_passed': clip_result.get('passed', False),
                        'self_check_consistent': self_check_result.get('is_consistent', False),
                        'keyword_penalty': keyword_result.get('penalty', 0),
                        'forbidden_keywords_detected': True,
                        'detected_forbidden_keywords': forbidden_keywords
                    },
                    'model': 'enhanced_multi_layer'
                }
            
            # 4. 최종 신뢰도 계산 (가중 평균)
            final_confidence = self._calculate_final_confidence(
                self_check_result, clip_result, keyword_result
            )
            
            # 5. 최종 판정
            verification_result = self._decide_verification_result(final_confidence)
            
            # 6. 상세 정보 구성
            verification_details = {
                'gemini_confidence': self_check_result.get('final_confidence', 0),
                'clip_similarity': clip_result.get('max_similarity', 0),
                'consistency_score': self_check_result.get('consistency_score', 0),
                'keyword_match': keyword_result.get('passed', False),
                'method': 'enhanced_multi_layer',
                'clip_passed': clip_result.get('passed', False),
                'self_check_consistent': self_check_result.get('is_consistent', False),
                'keyword_penalty': keyword_result.get('penalty', 0)
            }
            
            # 7. 통합 설명 생성
            explanation_data = self._generate_enhanced_explanation(
                self_check_result, clip_result, keyword_result, verification_result
            )
            
            logger.info(f"🎯 최종 결과 - 판정: {verification_result}, 신뢰도: {final_confidence:.3f}")
            
            return {
                'success': True,
                'verification_result': verification_result,
                'confidence': final_confidence,
                'explanation': explanation_data['ai_analysis'],
                'detected_items': explanation_data['detected_items'],
                'verification_details': verification_details,
                'model': 'enhanced_multi_layer'
            }
            
        except Exception as e:
            logger.error(f"다층 검증 시스템 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'enhanced_multi_layer'
            }
    
    def _calculate_final_confidence(self, self_check_result: Dict, clip_result: Dict, keyword_result: Dict) -> float:
        """최종 신뢰도 계산 (가중 평균)"""
        try:
            # 각 검증 레이어의 점수
            gemini_score = self_check_result.get('final_confidence', 0)
            consistency_score = self_check_result.get('consistency_score', 0)
            clip_score = clip_result.get('max_similarity', 0)
            keyword_score = 1.0 - keyword_result.get('penalty', 0)  # 감점을 점수로 변환
            
            # 가중 평균 (일관성 비중 증가)
            weights = {
                'gemini': 0.35,
                'consistency': 0.25,
                'clip': 0.25,
                'keyword': 0.15
            }
            
            final_confidence = (
                gemini_score * weights['gemini'] +
                consistency_score * weights['consistency'] +
                clip_score * weights['clip'] +
                keyword_score * weights['keyword']
            )
            
            # 0.0 ~ 1.0 범위로 제한
            final_confidence = max(0.0, min(1.0, final_confidence))
            
            # 상세 로그 출력
            logger.info(f"🧮 최종 신뢰도 계산 상세:")
            logger.info(f"   - gemini_score: {gemini_score:.3f} × {weights['gemini']} = {gemini_score * weights['gemini']:.3f}")
            logger.info(f"   - consistency_score: {consistency_score:.3f} × {weights['consistency']} = {consistency_score * weights['consistency']:.3f}")
            logger.info(f"   - clip_score: {clip_score:.3f} × {weights['clip']} = {clip_score * weights['clip']:.3f}")
            logger.info(f"   - keyword_score: {keyword_score:.3f} × {weights['keyword']} = {keyword_score * weights['keyword']:.3f}")
            logger.info(f"   - 최종 신뢰도: {final_confidence:.3f}")
            
            return final_confidence
            
        except Exception as e:
            logger.error(f"신뢰도 계산 오류: {e}")
            return 0.5  # 기본값
    
    def _decide_verification_result(self, confidence: float) -> str:
        """신뢰도 기반 최종 판정"""
        if confidence >= 0.8:
            return "APPROVED"
        elif confidence >= 0.5:
            return "NEEDS_REVIEW"
        else:
            return "REJECTED"
    
    def _generate_enhanced_explanation(self, self_check_result: Dict, clip_result: Dict, 
                                     keyword_result: Dict, verification_result: str) -> Dict[str, Any]:
        """통합 설명 생성 - 설명과 감지 항목을 분리하여 반환"""
        try:
            # AI 분석 설명만 추출
            ai_analysis = self_check_result.get('positive_result', {}).get('explanation', '')
            if not ai_analysis:
                ai_analysis = "다층 검증 시스템을 통한 종합 분석 완료"
            
            # 감지 항목들 수집
            detected_items = []
            
            # CLIP 결과
            if clip_result.get('passed', False):
                best_keyword = clip_result.get('best_keyword', '')
                if best_keyword:
                    detected_items.append({
                        'type': '객체 인식',
                        'value': f"{best_keyword} 확인됨",
                        'icon': '🔍'
                    })
            
            # 키워드 결과
            if keyword_result.get('passed', False):
                matched = keyword_result.get('matched_keywords', [])
                if matched:
                    detected_items.append({
                        'type': '키워드 검증',
                        'value': f"{', '.join(matched)} 확인됨",
                        'icon': '🔑'
                    })
            
            # 일관성 결과
            if self_check_result.get('is_consistent', False):
                detected_items.append({
                    'type': '일관성 검증',
                    'value': 'AI 판단이 일관됨',
                    'icon': '✅'
                })
            
            return {
                'ai_analysis': ai_analysis,
                'detected_items': detected_items
            }
            
        except Exception as e:
            logger.error(f"설명 생성 오류: {e}")
            return {
                'ai_analysis': "다층 검증 시스템을 통한 종합 분석 완료",
                'detected_items': []
            }
    
    def verify_challenge_image(self, image_data: bytes, challenge_title: str, challenge_code: str) -> Dict[str, Any]:
        """
        챌린지 이미지 인증 - 다층 검증 시스템 사용
        
        Args:
            image_data: 이미지 바이너리 데이터
            challenge_title: 챌린지 제목
            challenge_code: 챌린지 코드 (TUMBLER_CHALLENGE, REUSABLE_BAG 등)
        
        Returns:
            Dict with verification result, confidence, and explanation
        """
        try:
            # 다층 검증 시스템 사용
            enhanced_result = self.verify_challenge_image_enhanced(image_data, challenge_title, challenge_code)
            
            if enhanced_result.get('success', False):
                # 기존 API 호환성을 위해 필드명 조정
                return {
                    'success': True,
                    'verification_result': enhanced_result.get('verification_result', 'NEEDS_REVIEW'),
                    'confidence': enhanced_result.get('confidence', 0.5),
                    'explanation': enhanced_result.get('explanation', ''),
                    'detected_items': enhanced_result.get('detected_items', []),
                    'verification_details': enhanced_result.get('verification_details', {}),
                    'model': enhanced_result.get('model', 'enhanced_multi_layer')
                }
            else:
                # 다층 검증 실패 시 기존 방식으로 폴백
                logger.warning("다층 검증 실패, 기존 Gemini 단독 검증으로 폴백")
                return self._fallback_verification(image_data, challenge_title, challenge_code)
                
        except Exception as e:
            logger.error(f"챌린지 이미지 검증 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'enhanced_multi_layer'
            }
    
    def _fallback_verification(self, image_data: bytes, challenge_title: str, challenge_code: str) -> Dict[str, Any]:
        """기존 Gemini 단독 검증 (폴백용)"""
        try:
            from PIL import Image
            import io
            
            # 챌린지별 검증 프롬프트 매핑
            challenge_prompts = {
                'TUMBLER_CHALLENGE': {
                    'target': '텀블러 또는 개인 물병',
                    'details': '사람이 텀블러를 들고 있거나, 텀블러가 책상/테이블 위에 있는 모습'
                },
                'REUSABLE_BAG': {
                    'target': '장바구니 또는 에코백',
                    'details': '재사용 가능한 가방을 들고 있거나 사용하는 모습'
                },
                'REUSABLE_BAG_EXTENDED': {
                    'target': '친환경 장바구니',
                    'details': '친환경 소재의 장바구니나 에코백을 사용하는 모습'
                },
                'PLUGGING': {
                    'target': '쓰레기를 줍는 모습',
                    'details': '플로깅(줍깅) 활동 - 쓰레기를 주우면서 걷기나 조깅을 하는 모습, 쓰레기봉투나 집게를 들고 있는 모습'
                },
                'NO_PLASTIC': {
                    'target': '일회용품 대신 다회용품 사용',
                    'details': '일회용 플라스틱이 아닌 재사용 가능한 용기나 도구를 사용하는 모습'
                },
                'RECYCLE': {
                    'target': '분리수거하는 모습',
                    'details': '재활용품을 분리수거통에 버리거나, 분리수거를 위해 정리한 재활용품'
                }
            }
            
            # 챌린지에 맞는 프롬프트 가져오기
            challenge_info = challenge_prompts.get(challenge_code, {
                'target': challenge_title,
                'details': f'{challenge_title} 관련 활동'
            })
            
            prompt = f"""
당신은 에코 챌린지 사진 인증 전문가입니다. 제출된 사진이 "{challenge_title}" 챌린지의 인증 사진으로 적합한지 판단해주세요.

**챌린지 목표**: {challenge_info['target']}
**인증 기준**: {challenge_info['details']}

**판단 기준**:
1. APPROVED (승인): 챌린지 목표물이 명확하게 보이고, 인증 기준을 완벽히 충족하는 경우
   - 신뢰도 0.8 이상

2. NEEDS_REVIEW (검토 필요): 다음 중 하나에 해당하는 경우
   - 목표물이 보이지만 각도나 조명이 좋지 않아 명확하지 않음
   - 목표물이 작게 보이거나 일부만 보임
   - 인증 기준을 부분적으로만 충족
   - 신뢰도 0.5~0.8

3. REJECTED (거부): 다음 중 하나에 해당하는 경우
   - 챌린지와 전혀 관련 없는 사진
   - 목표물이 전혀 보이지 않음
   - 인터넷에서 다운받은 것으로 의심되는 사진
   - 의도적으로 속이려는 시도가 명확한 경우
   - 신뢰도 0.5 미만

반드시 다음 JSON 형식으로만 응답해주세요:
{{
    "verification_result": "APPROVED" | "NEEDS_REVIEW" | "REJECTED",
    "confidence": 0.0-1.0,
    "explanation": "판단 근거에 대한 한글 설명 (2-3문장)",
    "detected_items": ["감지된 물체 목록"]
}}
"""
            
            # 이미지 데이터를 PIL Image로 변환
            image = Image.open(io.BytesIO(image_data))
            logger.info(f"폴백 검증 - 이미지 정보: {image.size[0]}x{image.size[1]} pixels, mode: {image.mode}")
            
            logger.info(f"폴백 Gemini API 호출 시작...")
            response = self.vision_model.generate_content([prompt, image])
            text = response.text.strip()
            logger.info(f"폴백 Gemini 응답 받음: {len(text)} characters")
            
            # JSON 파싱
            if text.startswith('```json'):
                text = text[7:]
            if text.endswith('```'):
                text = text[:-3]
            text = text.strip()
            
            result = json.loads(text)
            
            return {
                'success': True,
                'verification_result': result.get('verification_result', 'NEEDS_REVIEW'),
                'confidence': result.get('confidence', 0.5),
                'explanation': result.get('explanation', ''),
                'detected_items': result.get('detected_items', []),
                'model': 'gemini-2.0-flash-fallback'
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"폴백 Gemini 챌린지 검증 JSON 파싱 오류: {str(e)}, 원본: {text}")
            return {
                'success': True,
                'verification_result': 'NEEDS_REVIEW',
                'confidence': 0.5,
                'explanation': 'AI가 이미지를 분석했으나 명확한 판단이 어려워 관리자 검토가 필요합니다.',
                'detected_items': [],
                'model': 'gemini-2.0-flash-fallback',
                'raw_response': text
            }
        except Exception as e:
            logger.error(f"폴백 Gemini 챌린지 이미지 검증 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gemini-2.0-flash-fallback'
            }

    def generate_quiz_with_prompt(self, prompt: str) -> Dict[str, Any]:
        """
        커스텀 프롬프트로 퀴즈 생성
        """
        try:
            response = self.model.generate_content(prompt)
            
            # JSON 파싱 시도
            try:
                import json
                quiz_data = json.loads(response.text)
                
                return {
                    'success': True,
                    'quiz': quiz_data.get('quiz', []),
                    'topic': quiz_data.get('topic', ''),
                    'difficulty': quiz_data.get('difficulty', 'medium'),
                    'source_info': quiz_data.get('source_info', ''),
                    'model': 'gemini-2.0-flash'
                }
            except json.JSONDecodeError:
                # JSON 파싱 실패 시 텍스트 그대로 반환
                return {
                    'success': True,
                    'quiz': [{
                        'question': '생성된 퀴즈',
                        'options': ['선택지1', '선택지2', '선택지3', '선택지4'],
                        'correct_answer': 0,
                        'explanation': response.text
                    }],
                    'topic': '커스텀',
                    'difficulty': 'medium',
                    'source_info': 'Gemini 생성',
                    'model': 'gemini-2.0-flash'
                }
                
        except Exception as e:
            logger.error(f"커스텀 프롬프트 퀴즈 생성 실패: {e}")
            return {
                'success': False,
                'error': str(e)
            }