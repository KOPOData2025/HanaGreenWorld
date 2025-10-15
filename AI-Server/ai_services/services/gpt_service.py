"""
OpenAI GPT API 서비스
"""
import openai
from django.conf import settings
import logging
import json
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class GPTService:
    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY가 설정되지 않았습니다.")
        
        openai.api_key = self.api_key
        self.client = openai.OpenAI(api_key=self.api_key)
    
    def generate_text(self, prompt: str, model: str = "gpt-3.5-turbo", **kwargs) -> Dict[str, Any]:
        """
        텍스트 생성
        """
        try:
            response = self.client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": "당신은 친환경 활동을 도와주는 AI 어시스턴트입니다."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=kwargs.get('max_tokens', 1000),
                temperature=kwargs.get('temperature', 0.7)
            )
            
            return {
                'success': True,
                'text': response.choices[0].message.content,
                'model': model,
                'usage': {
                    'prompt_tokens': response.usage.prompt_tokens,
                    'completion_tokens': response.usage.completion_tokens,
                    'total_tokens': response.usage.total_tokens
                }
            }
        except Exception as e:
            logger.error(f"GPT 텍스트 생성 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': model
            }
    
    def analyze_image(self, image_data: bytes, prompt: str = "이 이미지를 분석해주세요.") -> Dict[str, Any]:
        """
        이미지 분석 (GPT-4 Vision)
        """
        try:
            import base64
            
            # 이미지를 base64로 인코딩
            image_base64 = base64.b64encode(image_data).decode('utf-8')
            
            response = self.client.chat.completions.create(
                model="gpt-4-vision-preview",
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{image_base64}"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=1000
            )
            
            return {
                'success': True,
                'text': response.choices[0].message.content,
                'model': 'gpt-4-vision-preview',
                'usage': {
                    'prompt_tokens': response.usage.prompt_tokens,
                    'completion_tokens': response.usage.completion_tokens,
                    'total_tokens': response.usage.total_tokens
                }
            }
        except Exception as e:
            logger.error(f"GPT 이미지 분석 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gpt-4-vision-preview'
            }
    
    def generate_quiz_question(self) -> Dict[str, Any]:
        """
        퀴즈 문제 생성 (GPT-4)
        """
        try:
            system_prompt = """당신은 환경보호와 녹색금융 전문가이자 교육자입니다. 
            사용자들이 환경과 지속가능한 금융에 대해 학습할 수 있도록 
            교육적이고 실용적인 퀴즈를 생성하는 것이 목표입니다.
            
            다음 원칙을 따라 퀴즈를 생성해주세요:
            1. 명확하고 이해하기 쉬운 문제 문장
            2. 그럴듯한 오답 선택지 (너무 쉬우거나 어렵지 않게)
            3. 교육적 가치가 있는 상세한 해설
            4. 일상생활에서 접할 수 있는 실용적인 내용"""
            
            user_prompt = """환경보호와 녹색금융에 관련된 퀴즈를 다음 JSON 형식으로 생성해주세요.
            반드시 아래 형식을 정확히 지켜서 JSON만 응답해주세요:
            
            {
                "question": "문제 내용",
                "options": ["선택지1", "선택지2", "선택지3", "선택지4"],
                "correct_answer": 0,
                "explanation": "정답에 대한 자세한 설명"
            }
            
            주제는 다음 중 하나를 랜덤하게 선택해서 작성해주세요:
            
            환경 관련:
            1. 기후변화와 지구온난화
            2. 재활용과 자원순환  
            3. 친환경 생활습관
            4. 탄소중립과 신재생에너지
            5. 생물다양성과 생태계 보호
            6. 대기질과 수질 관리
            
            녹색금융 관련:
            7. ESG 투자와 지속가능 금융
            8. 그린본드와 친환경 금융상품
            9. 탄소배출권 거래와 탄소가격제
            10. 친환경 기업 투자와 평가
            11. 지속가능한 금융정책과 규제
            12. 기후 리스크와 금융 안정성
            13. 친환경 신용카드와 금융혜택
            14. 그린뱅킹과 디지털 금융
            
            예시 (환경 관련):
            {
                "question": "다음 중 탄소 발자국을 가장 많이 줄일 수 있는 방법은?",
                "options": ["대중교통 이용", "LED 전구 사용", "재활용 분리수거", "짧은 샤워하기"],
                "correct_answer": 0,
                "explanation": "대중교통 이용은 개인 자동차 대비 탄소배출량을 50-70% 줄일 수 있어 가장 효과적입니다."
            }
            
            예시 (녹색금융 관련):
            {
                "question": "ESG 투자에서 'E'는 무엇을 의미하나요?",
                "options": ["Economy(경제)", "Environment(환경)", "Ethics(윤리)", "Education(교육)"],
                "correct_answer": 1,
                "explanation": "ESG는 Environment(환경), Social(사회), Governance(지배구조)의 약자로, 지속가능한 투자를 평가하는 기준입니다."
            }
            
            난이도는 일반인이 이해할 수 있는 수준으로 작성하고,
            설명은 교육적이고 실용적인 내용으로 작성해주세요.
            특히 녹색금융 관련 문제는 일상생활에서 접할 수 있는 금융상품이나 정책에 대해 다뤄주세요."""
            
            response = self.client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                max_tokens=1000,
                temperature=0.7
            )
            
            # JSON 파싱 시도
            try:
                # 마크다운 코드 블록 제거
                text = response.choices[0].message.content.strip()
                if text.startswith('```json'):
                    text = text[7:]  # ```json 제거
                if text.endswith('```'):
                    text = text[:-3]  # ``` 제거
                text = text.strip()
                
                result = json.loads(text)
                return {
                    'success': True,
                    'quiz': result,
                    'model': 'gpt-4',
                    'usage': {
                        'prompt_tokens': response.usage.prompt_tokens,
                        'completion_tokens': response.usage.completion_tokens,
                        'total_tokens': response.usage.total_tokens
                    }
                }
            except json.JSONDecodeError as e:
                logger.error(f"JSON 파싱 실패: {e}, 원본 텍스트: {response.choices[0].message.content}")
                # JSON 파싱 실패 시 텍스트 그대로 반환
                return {
                    'success': True,
                    'quiz': {
                        'question': 'JSON 파싱 오류로 인한 기본 문제',
                        'options': ['선택지1', '선택지2', '선택지3', '선택지4'],
                        'correct_answer': 0,
                        'explanation': 'AI 응답 파싱 중 오류가 발생했습니다.'
                    },
                    'model': 'gpt-4',
                    'raw_response': response.choices[0].message.content,
                    'usage': {
                        'prompt_tokens': response.usage.prompt_tokens,
                        'completion_tokens': response.usage.completion_tokens,
                        'total_tokens': response.usage.total_tokens
                    }
                }
                
        except Exception as e:
            logger.error(f"GPT 퀴즈 생성 오류: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'model': 'gpt-4'
            }