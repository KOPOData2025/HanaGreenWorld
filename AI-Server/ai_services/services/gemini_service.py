"""
Gemini API 서비스
"""
import google.generativeai as genai
from django.conf import settings
import logging
import json
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class GeminiService:
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다.")
        
        genai.configure(api_key=self.api_key)
        self.model = genai.GenerativeModel('gemini-2.0-flash')
        self.vision_model = genai.GenerativeModel('gemini-2.0-flash')
    
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

    def verify_challenge_image(self, image_data: bytes, challenge_title: str, challenge_code: str) -> Dict[str, Any]:
        """
        챌린지 이미지 인증
        
        Args:
            image_data: 이미지 바이너리 데이터
            challenge_title: 챌린지 제목
            challenge_code: 챌린지 코드 (TUMBLER_CHALLENGE, REUSABLE_BAG 등)
        
        Returns:
            Dict with verification result, confidence, and explanation
        """
        try:
            logger.info(f"🔍 AI 검증 시작 - 챌린지: {challenge_title} ({challenge_code})")
            logger.info(f"📊 이미지 크기: {len(image_data)} bytes")
            
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
            logger.info(f"이미지 정보: {image.size[0]}x{image.size[1]} pixels, mode: {image.mode}")
            
            logger.info(f"Gemini API 호출 시작...")
            response = self.vision_model.generate_content([prompt, image])
            text = response.text.strip()
            logger.info(f"Gemini 응답 받음: {len(text)} characters")
            
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
                'model': 'gemini-2.0-flash'
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"Gemini 챌린지 검증 JSON 파싱 오류: {str(e)}, 원본: {text}")
            # JSON 파싱 실패 시 보수적으로 검토 필요 상태로 반환
            return {
                'success': True,
                'verification_result': 'NEEDS_REVIEW',
                'confidence': 0.5,
                'explanation': 'AI가 이미지를 분석했으나 명확한 판단이 어려워 관리자 검토가 필요합니다.',
                'detected_items': [],
                'model': 'gemini-2.0-flash',
                'raw_response': text
            }
        except Exception as e:
            logger.error(f"Gemini 챌린지 이미지 검증 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gemini-2.0-flash'
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