"""
RAG + GPT 하이브리드 퀴즈 생성 서비스
최신 친환경 뉴스를 기반으로 한 퀴즈 생성
"""
import logging
from typing import Dict, Any, List, Optional
from .vector_db_service import VectorDBService
from .gemini_service import GeminiService

logger = logging.getLogger(__name__)

class RAGQuizService:
    def __init__(self):
        self.vector_db = VectorDBService()
        self.gemini_service = GeminiService()
    
    def generate_rag_quiz(self, topic: str, difficulty: str = "medium", num_questions: int = 5) -> Dict[str, Any]:
        """
        RAG 기반 퀴즈 생성
        """
        try:
            logger.info(f"RAG 퀴즈 생성 시작 - 주제: {topic}, 난이도: {difficulty}")
            
            # 1. 관련 뉴스 검색
            relevant_articles = self.vector_db.search_similar_articles(
                query=topic, 
                n_results=10
            )
            
            if not relevant_articles:
                logger.warning("관련 뉴스를 찾을 수 없습니다. 기본 GPT 퀴즈를 생성합니다.")
                return self._generate_fallback_quiz(topic, difficulty, num_questions)
            
            # 2. 검색된 뉴스에서 컨텍스트 추출
            context = self._extract_context_from_articles(relevant_articles)
            
            # 3. RAG 프롬프트 생성
            rag_prompt = self._create_rag_prompt(topic, difficulty, num_questions, context)
            
            # 4. Gemini로 퀴즈 생성
            quiz_result = self.gemini_service.generate_quiz_with_prompt(rag_prompt)
            
            # 5. RAG 정보 추가
            quiz_result['rag_info'] = {
                'used_rag': True,
                'relevant_articles_count': len(relevant_articles),
                'sources': list(set([article['metadata']['source'] for article in relevant_articles])),
                'context_length': len(context),
                'generation_method': 'RAG + GPT Hybrid'
            }
            
            logger.info(f"RAG 퀴즈 생성 완료 - {num_questions}개 문제")
            return quiz_result
            
        except Exception as e:
            logger.error(f"RAG 퀴즈 생성 실패: {e}")
            return self._generate_fallback_quiz(topic, difficulty, num_questions)
    
    def _extract_context_from_articles(self, articles: List[Dict[str, Any]]) -> str:
        """
        검색된 기사들에서 컨텍스트 추출
        """
        context_parts = []
        
        for i, article in enumerate(articles[:5], 1):  # 상위 5개 기사만 사용
            metadata = article['metadata']
            content = article['content']
            
            context_part = f"""
[기사 {i}]
출처: {metadata['source']}
제목: {metadata['title']}
날짜: {metadata['date']}
내용: {content[:500]}...
"""
            context_parts.append(context_part)
        
        return "\n".join(context_parts)
    
    def _create_rag_prompt(self, topic: str, difficulty: str, num_questions: int, context: str) -> str:
        """
        RAG 기반 프롬프트 생성 (강화된 프롬프팅)
        """
        prompt = f"""
당신은 환경보호와 녹색금융 전문가입니다. 아래 제공된 최신 친환경 뉴스 정보를 바탕으로 교육적이고 실용적인 퀴즈를 생성해주세요.

**주제**: {topic}
**난이도**: {difficulty}
**문제 수**: {num_questions}개

**최신 뉴스 정보 (RAG 검색 결과)**:
{context}

**반드시 다음 JSON 형식으로만 응답해주세요**:
{{
    "quiz": [
        {{
            "question": "문제 내용",
            "options": ["선택지1", "선택지2", "선택지3", "선택지4"],
            "correct_answer": 0,
            "explanation": "정답에 대한 자세한 설명"
        }}
    ],
    "topic": "{topic}",
    "difficulty": "{difficulty}",
    "source_info": "RAG 기반 최신 뉴스 정보 활용"
}}

**퀴즈 생성 원칙 (엄격히 준수)**:
1. **뉴스 정보 우선 활용**: 위 뉴스에서 언급된 구체적인 사실, 통계, 정책, 날짜, 숫자를 반드시 포함
2. **최신성 보장**: 2024-2025년 최신 정보를 바탕으로 한 문제만 생성
3. **정확성 검증**: 뉴스 내용과 일치하는 정답만 제공
4. **교육적 가치**: 단순 암기가 아닌 이해와 적용을 요구하는 문제
5. **실용성**: 일상생활에서 활용 가능한 정보 포함

**주제별 가이드라인**:
환경 관련 (1-6번 중 선택):
1. 기후변화와 지구온난화 - 온실가스, 지구온도 상승, 기후변화 영향
2. 재활용과 자원순환 - 분리수거, 업사이클링, 순환경제
3. 친환경 생활습관 - 에너지 절약, 물 절약, 친환경 제품 사용
4. 탄소중립과 신재생에너지 - 태양광, 풍력, 탄소중립 정책
5. 생물다양성과 생태계 보호 - 멸종위기종, 생태계 복원, 서식지 보호
6. 대기질과 수질 관리 - 미세먼지, 수질오염, 환경기준

녹색금융 관련 (7-14번 중 선택):
7. ESG 투자와 지속가능 금융 - ESG 평가, 지속가능 투자 상품
8. 그린본드와 친환경 금융상품 - 그린본드, 친환경 펀드, ESG 펀드
9. 탄소배출권 거래와 탄소가격제 - K-ETS, 탄소세, 배출권 거래
10. 친환경 기업 투자와 평가 - 그린기업 인증, 친환경 기업 평가
11. 지속가능한 금융정책과 규제 - 그린뉴딜, 탄소중립 정책
12. 기후 리스크와 금융 안정성 - 물리적 리스크, 전환 리스크
13. 친환경 신용카드와 금융혜택 - 그린카드, 친환경 혜택, 포인트 적립
14. 그린뱅킹과 디지털 금융 - 디지털 금융, 그린뱅킹 서비스

**퀴즈 작성 원칙 (필수)**:
1. 문제는 명확하고 이해하기 쉬워야 함
2. 선택지는 모두 그럴듯해야 함 (너무 쉬우거나 어렵지 않게)
3. 오답 선택지는 교육적 가치가 있어야 함
4. 해설은 구체적이고 실용적인 정보를 포함해야 함
5. 한국의 정책이나 사례를 우선적으로 언급
6. **뉴스에서 언급된 구체적인 수치, 날짜, 정책명을 반드시 활용**

**난이도별 기준**:
- **쉬움**: 기본 개념, 일반적 상식
- **보통**: 구체적 정책, 통계, 최신 동향
- **어려움**: 전문적 지식, 복합적 이해, 최신 연구 결과

**예시 (뉴스 기반)**:
{{
    "question": "2024년 한국 정부가 발표한 탄소중립 정책에서 2030년까지 달성할 목표는?",
    "options": ["온실가스 40% 감축", "온실가스 30% 감축", "온실가스 50% 감축", "온실가스 20% 감축"],
    "correct_answer": 0,
    "explanation": "2024년 정부 발표에 따르면 2030년까지 온실가스 배출량을 2018년 대비 40% 감축하는 것이 목표입니다. 이는 NDC(국가온실가스감축목표)에 명시된 구체적인 수치입니다."
}}

**중요**: 위 뉴스 정보를 반드시 참조하여 정확하고 최신의 퀴즈를 생성해주세요. 뉴스에 없는 내용은 추측하지 마세요.
"""
        return prompt
    
    def _generate_fallback_quiz(self, topic: str, difficulty: str, num_questions: int) -> Dict[str, Any]:
        """
        RAG 실패 시 기본 Gemini 퀴즈 생성
        """
        logger.info("RAG 실패, 기본 Gemini 퀴즈 생성")
        
        fallback_prompt = f"""
당신은 환경보호와 녹색금융 전문가입니다. 교육적이고 실용적인 퀴즈를 생성해주세요.

**주제**: {topic}
**난이도**: {difficulty}
**문제 수**: {num_questions}개

**반드시 다음 JSON 형식으로만 응답해주세요**:
{{
    "quiz": [
        {{
            "question": "문제 내용",
            "options": ["선택지1", "선택지2", "선택지3", "선택지4"],
            "correct_answer": 0,
            "explanation": "정답에 대한 자세한 설명"
        }}
    ],
    "topic": "{topic}",
    "difficulty": "{difficulty}",
    "source_info": "Gemini 기본 생성"
}}

**퀴즈 생성 원칙**:
1. 문제는 명확하고 이해하기 쉬워야 함
2. 선택지는 모두 그럴듯해야 함
3. 해설은 구체적이고 실용적인 정보를 포함
4. 한국의 정책이나 사례를 우선적으로 언급
5. 친환경과 금융이 연관된 내용 포함

**주제별 가이드라인**:
환경 관련: 기후변화, 재활용, 친환경 생활습관, 탄소중립, 신재생에너지, 생물다양성
녹색금융 관련: ESG 투자, 그린본드, 탄소배출권, 친환경 기업 평가, 그린뉴딜, 그린카드
"""
        
        quiz_result = self.gemini_service.generate_quiz_with_prompt(fallback_prompt)
        quiz_result['rag_info'] = {
            'used_rag': False,
            'generation_method': 'Gemini Only (Fallback)',
            'reason': 'RAG 검색 실패'
        }
        
        return quiz_result
    
    def update_news_data(self) -> bool:
        """
        뉴스 데이터 업데이트 (기존 네이버 뉴스 데이터 사용)
        """
        try:
            logger.info("뉴스 데이터 업데이트 시작...")
            
            # 1. 기존 네이버 뉴스 데이터 로드
            import json
            try:
                with open('naver_news_articles.json', 'r', encoding='utf-8') as f:
                    new_articles = json.load(f)
                logger.info(f"기존 네이버 뉴스 {len(new_articles)}개 로드 완료")
            except FileNotFoundError:
                logger.error("naver_news_articles.json 파일을 찾을 수 없습니다.")
                return False
            
            if not new_articles:
                logger.warning("뉴스 데이터가 없습니다.")
                return False
            
            # 2. 벡터 데이터베이스에 추가
            success = self.vector_db.add_articles_to_vector_db(new_articles)
            
            if success:
                logger.info(f"뉴스 데이터 업데이트 완료: {len(new_articles)}개 기사")
                return True
            else:
                logger.error("벡터 데이터베이스 업데이트 실패")
                return False
                
        except Exception as e:
            logger.error(f"뉴스 데이터 업데이트 실패: {e}")
            return False
    
    def get_quiz_with_latest_news(self, topic: str, difficulty: str = "medium") -> Dict[str, Any]:
        """
        최신 뉴스 기반 퀴즈 생성 (자동 업데이트 포함)
        """
        try:
            # 1. 최신 뉴스 데이터 확인/업데이트
            self.update_news_data()
            
            # 2. RAG 기반 퀴즈 생성
            quiz_result = self.generate_rag_quiz(topic, difficulty, num_questions=5)
            
            # 3. 추가 정보 포함
            stats = self.vector_db.get_collection_stats()
            quiz_result['database_info'] = {
                'total_articles': stats.get('total_articles', 0),
                'source_distribution': stats.get('source_distribution', {}),
                'last_updated': stats.get('last_updated', 'Unknown')
            }
            
            return quiz_result
            
        except Exception as e:
            logger.error(f"최신 뉴스 기반 퀴즈 생성 실패: {e}")
            return self._generate_fallback_quiz(topic, difficulty, 5)
    
    def search_related_news(self, query: str, limit: int = 5) -> List[Dict[str, Any]]:
        """
        관련 뉴스 검색
        """
        try:
            results = self.vector_db.search_similar_articles(query, n_results=limit)
            
            # 결과 정리
            news_list = []
            for result in results:
                news_item = {
                    'title': result['metadata']['title'],
                    'source': result['metadata']['source'],
                    'date': result['metadata']['date'],
                    'url': result['metadata']['url'],
                    'similarity_score': result['similarity_score'],
                    'preview': result['content'][:200] + "..."
                }
                news_list.append(news_item)
            
            return news_list
            
        except Exception as e:
            logger.error(f"뉴스 검색 실패: {e}")
            return []

# 테스트용 함수
def test_rag_quiz():
    """
    RAG 퀴즈 서비스 테스트
    """
    try:
        # RAG 퀴즈 서비스 초기화
        rag_quiz = RAGQuizService()
        
        # 1. 뉴스 데이터 업데이트
        update_success = rag_quiz.update_news_data()
        print(f"업데이트 결과: {'성공' if update_success else '실패'}")
        
        # 2. RAG 퀴즈 생성 테스트
        quiz_result = rag_quiz.get_quiz_with_latest_news("친환경 에너지", "medium")
        
        if quiz_result and 'quiz' in quiz_result:
            print(f"퀴즈 생성 성공!")
            print(f"- 문제 수: {len(quiz_result['quiz'])}개")
            print(f"- RAG 사용: {quiz_result['rag_info']['used_rag']}")
            print(f"- 관련 기사 수: {quiz_result['rag_info']['relevant_articles_count']}")
            print(f"- 데이터베이스 기사 수: {quiz_result['database_info']['total_articles']}개")
            
            # 첫 번째 문제 출력
            if quiz_result['quiz']:
                first_question = quiz_result['quiz'][0]
                print(f"\n 첫 번째 문제:")
                print(f"Q: {first_question['question']}")
                print(f"A: {first_question['correct_answer']}")
        else:
            print("❌ 퀴즈 생성 실패")
        
        # 3. 뉴스 검색 테스트
        news_results = rag_quiz.search_related_news("탄소중립", limit=3)
        print(f"검색 결과: {len(news_results)}개")
        for i, news in enumerate(news_results, 1):
            print(f"{i}. {news['title']} ({news['source']})")
        
        return True
        
    except Exception as e:
        print(f"❌ RAG 퀴즈 서비스 테스트 실패: {e}")
        return False

if __name__ == "__main__":
    test_rag_quiz()
