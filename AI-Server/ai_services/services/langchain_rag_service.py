import logging
from typing import Dict, Any, List, Optional
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_core.prompts import ChatPromptTemplate
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_community.document_loaders import TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from django.conf import settings
import json
import os

logger = logging.getLogger(__name__)

class LangChainRAGService:
    def __init__(self):
        # OpenAI API 설정
        self.api_key = settings.OPENAI_API_KEY
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY가 설정되지 않았습니다.")
        
        # LangChain 컴포넌트 초기화
        self.llm = ChatOpenAI(
            model="gpt-4o-mini",
            temperature=0.7,
            api_key=self.api_key
        )
        
        self.embeddings = OpenAIEmbeddings(
            model="text-embedding-3-small",
            api_key=self.api_key
        )
        
        # 벡터 스토어 설정
        self.persist_directory = "./chroma_db"
        self.vectorstore = None
        self.retriever = None
        self.rag_chain = None
        
        # 텍스트 분할기 설정
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1000,
            chunk_overlap=200
        )
        
        # 프롬프트 템플릿 설정
        self._setup_prompt_template()
        
        # 벡터 스토어 초기화
        self._initialize_vectorstore()
    
    def _setup_prompt_template(self):
        """
        일일 퀴즈용 프롬프트 템플릿 설정 (쉬움-보통 난이도)
        """
        self.prompt_template = """
당신은 친환경 활동과 녹색금융 전문가입니다. 
사용자가 제공한 최신 뉴스 정보를 바탕으로 **일일 퀴즈**용 객관식 문제를 생성해주세요.

**중요**: 일일 퀴즈는 일반 사용자들이 쉽게 참여할 수 있어야 하므로 **쉬움-보통 난이도**로만 만들어주세요.

--- 최신 뉴스 정보 ---
{context}
---

요청 주제: {input}

**반드시 다음 JSON 형식으로만 응답해주세요**:
{{
    "quiz": [
        {{
            "question": "문제 내용 (쉬움-보통 난이도)",
            "options": ["선택지1", "선택지2", "선택지3", "선택지4"],
            "correct_answer": 0,
            "explanation": "정답에 대한 간단하고 이해하기 쉬운 설명"
        }}
    ],
    "topic": "요청 주제",
    "difficulty": "쉬움-보통",
    "source_info": "최신 뉴스 기반"
}}

**퀴즈 생성 원칙 (일일 퀴즈용)**:
1. **쉬움-보통 난이도**: 일반인이 쉽게 이해할 수 있지만 약간의 사고가 필요한 수준
2. **구체적 숫자 금지**: 정확한 금액, 날짜, 수치 등은 피하고 개념적 이해 중심
3. **교육적 가치**: 단순 암기가 아닌 이해를 돕는 문제
4. **실용성**: 일상생활에서 활용 가능한 정보
5. **명확한 선택지**: 헷갈리지 않는 명확한 선택지 제공
6. **시점 명시**: 언제의 정보인지 반드시 포함
7. **적절한 난이도**: 너무 쉬운 상식 문제보다는 최신 트렌드나 정책 관련 문제

**주제별 가이드라인**:
환경 관련: 기후변화, 재활용, 친환경 생활습관, 탄소중립, 신재생에너지
녹색금융 관련: ESG 투자, 그린본드, 친환경 카드, 그린뉴딜, 탄소배출권

**예시 (일일 퀴즈 수준)**:
{{
    "question": "다음 중 탄소 발자국을 줄이는 가장 효과적인 방법은?",
    "options": ["대중교통 이용", "전기차 구매", "태양광 패널 설치", "친환경 제품 구매"],
    "correct_answer": 0,
    "explanation": "대중교통 이용은 추가 비용 없이 바로 탄소 배출을 줄일 수 있는 가장 효과적이고 접근하기 쉬운 방법입니다."
}}

**잘못된 예시 (피해야 할 유형)**:
{{
    "question": "친환경차 보급을 늘리기 위한 정부의 주요 정책은?",
    "options": ["내연기관차를 폐차하거나 교체하는 것", "전기차를 판매하는 것", "친환경차 가격 인상", "자동차 보험료 인상"],
    "correct_answer": 0,
    "explanation": "이런 문제는 0번과 1번이 모두 맞는 내용이라 애매합니다."
}}

**올바른 예시**:
{{
    "question": "다음 중 탄소 배출을 가장 많이 줄일 수 있는 교통수단은?",
    "options": ["대중교통 이용", "전기차 운전", "하이브리드차 운전", "내연기관차 운전"],
    "correct_answer": 0,
    "explanation": "대중교통은 여러 사람이 함께 이용하므로 개인당 탄소 배출량이 가장 적습니다."
}}

**피해야 할 문제 유형**:
- 정확한 금액이나 수치를 묻는 문제 (예: "15조9160억 원")
- 구체적인 날짜나 기간을 묻는 문제
- 전문적인 용어나 복잡한 개념
- 암기 위주의 문제
- 너무 쉬운 상식 문제 (예: "친환경 금융의 목적은?")

**추천하는 문제 유형**:
- 최신 정책이나 트렌드 관련 문제
- 실생활에서 적용 가능한 친환경 방법
- ESG 투자나 녹색금융의 구체적 사례
- 환경 보호와 경제 성장의 연관성

**선택지 작성 원칙 (매우 중요)**:
- 정답은 명확하고 유일해야 함
- 오답은 확실히 틀린 내용이어야 함
- 애매하거나 부분적으로 맞는 선택지 금지
- 헷갈릴 수 있는 유사한 선택지 피하기
- 정답과 오답 사이에 명확한 구분이 있어야 함
- 오답은 논리적으로 틀리거나 관련 없는 내용이어야 함

**선택지 검증 체크리스트**:
1. 정답이 유일한가?
2. 오답들이 확실히 틀린가?
3. 선택지들 사이에 애매함이 없는가?
4. 사용자가 헷갈릴 수 있는가?

위 뉴스 정보를 참고하여 정확하고 교육적인 일일 퀴즈를 생성해주세요.
"""
        
        self.quiz_prompt = ChatPromptTemplate.from_template(self.prompt_template)
    
    def _initialize_vectorstore(self):
        """
        벡터 스토어 초기화
        """
        try:
            # 기존 벡터 스토어 로드 시도
            if os.path.exists(self.persist_directory):
                self.vectorstore = Chroma(
                    persist_directory=self.persist_directory,
                    embedding_function=self.embeddings
                )
                logger.info("기존 벡터 스토어 로드 완료")
            else:
                # 빈 벡터 스토어 생성
                self.vectorstore = Chroma(
                    persist_directory=self.persist_directory,
                    embedding_function=self.embeddings
                )
                logger.info("새 벡터 스토어 생성 완료")
            
            # 검색기 설정
            self.retriever = self.vectorstore.as_retriever(
                search_kwargs={"k": 5}  # 상위 5개 문서 검색
            )
            
            # RAG 체인 구성
            document_chain = create_stuff_documents_chain(self.llm, self.quiz_prompt)
            self.rag_chain = create_retrieval_chain(self.retriever, document_chain)
            
            logger.info("LangChain RAG 체인 초기화 완료")
            
        except Exception as e:
            logger.error(f"벡터 스토어 초기화 실패: {e}")
            raise
    
    def add_news_to_vectorstore(self, articles: List[Dict[str, Any]]) -> bool:
        """
        뉴스 기사들을 벡터 스토어에 추가
        """
        try:
            if not articles:
                logger.warning("추가할 기사가 없습니다.")
                return False
            
            # 문서 변환
            documents = []
            for article in articles:
                # 제목 + 내용을 하나의 텍스트로 결합
                text = f"제목: {article['title']}\n출처: {article['source']}\n날짜: {article['date']}\n내용: {article['content']}"
                
                # LangChain Document 객체 생성
                from langchain_core.documents import Document
                doc = Document(
                    page_content=text,
                    metadata={
                        'title': article['title'],
                        'source': article['source'],
                        'date': article['date'],
                        'url': article['url']
                    }
                )
                documents.append(doc)
            
            # 텍스트 분할
            chunks = self.text_splitter.split_documents(documents)
            
            # 벡터 스토어에 추가
            self.vectorstore.add_documents(chunks)
            
            # 검색기 업데이트
            self.retriever = self.vectorstore.as_retriever(
                search_kwargs={"k": 5}
            )
            
            # RAG 체인 재구성
            document_chain = create_stuff_documents_chain(self.llm, self.quiz_prompt)
            self.rag_chain = create_retrieval_chain(self.retriever, document_chain)
            
            logger.info(f"뉴스 {len(articles)}개를 벡터 스토어에 추가 완료")
            return True
            
        except Exception as e:
            logger.error(f"뉴스 추가 실패: {e}")
            return False
    
    def generate_daily_quiz(self, topic: str = "친환경 활동") -> Dict[str, Any]:
        """
        일일 퀴즈 생성 (쉬움-보통 난이도)
        """
        try:
            logger.info(f"일일 퀴즈 생성 시작 - 주제: {topic}")
            
            # RAG 체인 실행
            user_query = f"{topic}에 대한 일일 퀴즈를 생성해주세요. 쉬움-보통 난이도로 만들어주세요."
            
            response = self.rag_chain.invoke({"input": user_query})
            
            # JSON 파싱 시도
            try:
                quiz_data = json.loads(response["answer"])
                
                result = {
                    'success': True,
                    'quiz': quiz_data.get('quiz', []),
                    'topic': quiz_data.get('topic', topic),
                    'difficulty': '쉬움-보통',
                    'source_info': quiz_data.get('source_info', '최신 뉴스 기반'),
                    'model': 'gpt-4o-mini',
                    'rag_info': {
                        'used_rag': True,
                        'generation_method': 'LangChain RAG',
                        'context_documents': len(response.get('context', []))
                    }
                }
                
                logger.info(f"일일 퀴즈 생성 완료 - {len(result['quiz'])}개 문제")
                return result
                
            except json.JSONDecodeError:
                # JSON 파싱 실패 시 텍스트 그대로 반환
                return {
                    'success': True,
                    'quiz': [{
                        'question': '일일 퀴즈',
                        'options': ['선택지1', '선택지2', '선택지3', '선택지4'],
                        'correct_answer': 0,
                        'explanation': response["answer"]
                    }],
                    'topic': topic,
                    'difficulty': '쉬움-보통',
                    'source_info': 'LangChain RAG 생성',
                    'model': 'gpt-4o-mini',
                    'rag_info': {
                        'used_rag': True,
                        'generation_method': 'LangChain RAG',
                        'context_documents': len(response.get('context', []))
                    }
                }
                
        except Exception as e:
            logger.error(f"일일 퀴즈 생성 실패: {e}")
            return {
                'success': False,
                'error': str(e),
                'quiz': []
            }
    
    def get_vectorstore_stats(self) -> Dict[str, Any]:
        """
        벡터 스토어 통계 조회
        """
        try:
            # 문서 개수 조회
            all_docs = self.vectorstore.get()
            total_docs = len(all_docs['ids']) if all_docs['ids'] else 0
            
            # 소스별 통계
            source_stats = {}
            if all_docs['metadatas']:
                for metadata in all_docs['metadatas']:
                    source = metadata.get('source', 'Unknown')
                    source_stats[source] = source_stats.get(source, 0) + 1
            
            return {
                'total_documents': total_docs,
                'source_distribution': source_stats,
                'persist_directory': self.persist_directory,
                'embedding_model': 'text-embedding-3-small',
                'llm_model': 'gpt-4o-mini'
            }
            
        except Exception as e:
            logger.error(f"벡터 스토어 통계 조회 실패: {e}")
            return {}
    
    def search_similar_documents(self, query: str, k: int = 5) -> List[Dict[str, Any]]:
        """
        유사한 문서 검색
        """
        try:
            docs = self.vectorstore.similarity_search(query, k=k)
            
            results = []
            for doc in docs:
                result = {
                    'content': doc.page_content[:200] + "...",
                    'metadata': doc.metadata,
                    'relevance_score': 1.0  # LangChain은 기본적으로 점수를 제공하지 않음
                }
                results.append(result)
            
            return results
            
        except Exception as e:
            logger.error(f"문서 검색 실패: {e}")
            return []

# 테스트용 함수
def test_langchain_rag():
    """
    LangChain RAG 서비스 테스트
    """
    try:
        print("🧪 LangChain RAG 서비스 테스트 시작...")
        
        # 서비스 초기화
        rag_service = LangChainRAGService()
        
        # 통계 확인
        stats = rag_service.get_vectorstore_stats()
        print(f"📊 벡터 스토어 통계:")
        print(f"- 총 문서 수: {stats.get('total_documents', 0)}개")
        print(f"- 소스별 분포: {stats.get('source_distribution', {})}")
        
        # 일일 퀴즈 생성 테스트
        print("\n🎯 일일 퀴즈 생성 테스트...")
        quiz_result = rag_service.generate_daily_quiz("친환경 에너지")
        
        if quiz_result['success'] and quiz_result['quiz']:
            print(f"일일 퀴즈 생성 성공!")
            print(f"- 문제 수: {len(quiz_result['quiz'])}개")
            print(f"- 난이도: {quiz_result['difficulty']}")
            print(f"- RAG 사용: {quiz_result['rag_info']['used_rag']}")
            
            # 첫 번째 문제 출력
            first_question = quiz_result['quiz'][0]
            print(f"\n첫 번째 문제:")
            print(f"Q: {first_question['question']}")
            print(f"A: {first_question['correct_answer']}")
            print(f"설명: {first_question['explanation']}")
        else:
            print("❌ 일일 퀴즈 생성 실패")
        
        return True
        
    except Exception as e:
        print(f"❌ LangChain RAG 서비스 테스트 실패: {e}")
        return False

if __name__ == "__main__":
    test_langchain_rag()
