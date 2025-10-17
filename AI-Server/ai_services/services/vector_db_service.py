"""
ChromaDB 벡터 데이터베이스 서비스
친환경 뉴스 데이터를 벡터화하여 저장하고 검색
"""
import chromadb
from chromadb.config import Settings
import openai
from django.conf import settings
import logging
import json
from typing import List, Dict, Any, Optional
import os
from datetime import datetime

logger = logging.getLogger(__name__)

class VectorDBService:
    def __init__(self):
        # OpenAI API 키 설정 (임베딩용)
        self.api_key = settings.OPENAI_API_KEY
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY가 설정되지 않았습니다.")
        
        openai.api_key = self.api_key
        self.client = openai.OpenAI(api_key=self.api_key)
        
        # ChromaDB 설정
        self.chroma_client = chromadb.Client(Settings(
            persist_directory="./chroma_db",
            anonymized_telemetry=False
        ))
        
        # 컬렉션 생성 또는 가져오기
        self.collection_name = "green_news_collection"
        try:
            self.collection = self.chroma_client.get_collection(name=self.collection_name)
            logger.info(f"기존 컬렉션 '{self.collection_name}' 로드 완료")
        except:
            self.collection = self.chroma_client.create_collection(
                name=self.collection_name,
                metadata={"description": "친환경 뉴스 및 금융 뉴스 벡터 데이터"}
            )
            logger.info(f"새 컬렉션 '{self.collection_name}' 생성 완료")
    
    def create_embeddings(self, texts: List[str]) -> List[List[float]]:
        """
        텍스트 리스트를 임베딩으로 변환 (OpenAI Batch API 사용 - 50% 할인)
        """
        try:
            # Batch API 사용으로 50% 할인 ($0.02 → $0.01 per 1M tokens)
            response = self.client.embeddings.create(
                model="text-embedding-3-small",
                input=texts,
                extra_headers={"OpenAI-Beta": "batch=v1"}  # Batch API 사용
            )
            return [embedding.embedding for embedding in response.data]
        except Exception as e:
            logger.error(f"OpenAI 임베딩 생성 실패: {e}")
            return []
    
    def add_articles_to_vector_db(self, articles: List[Dict[str, Any]]) -> bool:
        """
        뉴스 기사들을 벡터 데이터베이스에 추가
        """
        try:
            if not articles:
                logger.warning("추가할 기사가 없습니다.")
                return False
            
            # 기존 데이터 확인
            existing_count = self.collection.count()
            logger.info(f"기존 벡터 데이터: {existing_count}개")
            
            # 중복 제거를 위한 기존 ID 확인
            existing_ids = set()
            if existing_count > 0:
                existing_data = self.collection.get()
                existing_ids = set(existing_data['ids'])
            
            # 새로 추가할 기사들 필터링
            new_articles = []
            for article in articles:
                article_id = f"{article['source']}_{article['title'][:50]}"
                if article_id not in existing_ids:
                    new_articles.append(article)
            
            if not new_articles:
                logger.info("새로 추가할 기사가 없습니다.")
                return True
            
            logger.info(f"새로 추가할 기사: {len(new_articles)}개")
            
            # 배치 처리 (한 번에 100개씩)
            batch_size = 100
            for i in range(0, len(new_articles), batch_size):
                batch = new_articles[i:i + batch_size]
                self._process_batch(batch)
            
            # 최종 데이터 개수 확인
            final_count = self.collection.count()
            logger.info(f"벡터 데이터베이스 업데이트 완료: {final_count}개")
            
            return True
            
        except Exception as e:
            logger.error(f"벡터 데이터베이스 추가 실패: {e}")
            return False
    
    def _process_batch(self, articles: List[Dict[str, Any]]):
        """
        배치 단위로 기사 처리
        """
        try:
            # 텍스트 준비
            texts = []
            ids = []
            metadatas = []
            
            for article in articles:
                # 제목 + 내용을 하나의 텍스트로 결합
                text = f"제목: {article['title']}\n내용: {article['content']}"
                texts.append(text)
                
                # 고유 ID 생성
                article_id = f"{article['source']}_{article['title'][:50]}_{datetime.now().strftime('%Y%m%d')}"
                ids.append(article_id)
                
                # 메타데이터 준비
                metadata = {
                    'title': article['title'],
                    'source': article['source'],
                    'date': article['date'],
                    'url': article['url'],
                    'content_length': len(article['content']),
                    'added_at': datetime.now().isoformat()
                }
                metadatas.append(metadata)
            
            # 임베딩 생성
            logger.info(f"임베딩 생성 중... ({len(texts)}개)")
            embeddings = self.create_embeddings(texts)
            
            if not embeddings:
                logger.error("임베딩 생성 실패")
                return
            
            # ChromaDB에 추가
            self.collection.add(
                embeddings=embeddings,
                documents=texts,
                metadatas=metadatas,
                ids=ids
            )
            
            logger.info(f"배치 처리 완료: {len(articles)}개 기사 추가")
            
        except Exception as e:
            logger.error(f"배치 처리 실패: {e}")
    
    def search_similar_articles(self, query: str, n_results: int = 5) -> List[Dict[str, Any]]:
        """
        쿼리와 유사한 기사 검색
        """
        try:
            # 쿼리 임베딩 생성
            query_embedding = self.create_embeddings([query])
            if not query_embedding:
                return []
            
            # 유사도 검색
            results = self.collection.query(
                query_embeddings=query_embedding,
                n_results=n_results,
                include=['documents', 'metadatas', 'distances']
            )
            
            # 결과 정리
            similar_articles = []
            if results['documents'] and results['documents'][0]:
                for i, doc in enumerate(results['documents'][0]):
                    article = {
                        'content': doc,
                        'metadata': results['metadatas'][0][i],
                        'similarity_score': 1 - results['distances'][0][i]  # 거리를 유사도로 변환
                    }
                    similar_articles.append(article)
            
            logger.info(f"검색 완료: {len(similar_articles)}개 결과")
            return similar_articles
            
        except Exception as e:
            logger.error(f"검색 실패: {e}")
            return []
    
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        컬렉션 통계 정보 반환
        """
        try:
            count = self.collection.count()
            
            # 소스별 통계
            all_data = self.collection.get(include=['metadatas'])
            source_stats = {}
            
            if all_data['metadatas']:
                for metadata in all_data['metadatas']:
                    source = metadata.get('source', 'Unknown')
                    source_stats[source] = source_stats.get(source, 0) + 1
            
            return {
                'total_articles': count,
                'source_distribution': source_stats,
                'collection_name': self.collection_name,
                'last_updated': datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"통계 조회 실패: {e}")
            return {}
    
    def clear_collection(self) -> bool:
        """
        컬렉션 전체 삭제
        """
        try:
            self.chroma_client.delete_collection(name=self.collection_name)
            self.collection = self.chroma_client.create_collection(
                name=self.collection_name,
                metadata={"description": "친환경 뉴스 및 금융 뉴스 벡터 데이터"}
            )
            logger.info("컬렉션 초기화 완료")
            return True
        except Exception as e:
            logger.error(f"컬렉션 초기화 실패: {e}")
            return False

# 테스트용 함수
def test_vector_db():
    """
    벡터 데이터베이스 테스트
    """
    try:
        # 벡터 DB 서비스 초기화
        vector_db = VectorDBService()
        
        # 통계 확인
        stats = vector_db.get_collection_stats()
        print(f"벡터 데이터베이스 통계:")
        print(f"- 총 기사 수: {stats.get('total_articles', 0)}개")
        print(f"- 소스별 분포: {stats.get('source_distribution', {})}")
        
        # 테스트 검색
        test_query = "친환경 에너지 정책"
        results = vector_db.search_similar_articles(test_query, n_results=3)
        
        print(f"\n테스트 검색 결과 ('{test_query}'):")
        for i, result in enumerate(results, 1):
            print(f"{i}. {result['metadata']['title']}")
            print(f"   출처: {result['metadata']['source']}")
            print(f"   유사도: {result['similarity_score']:.3f}")
            print(f"   내용: {result['content'][:100]}...")
            print()
        
        return True
        
    except Exception as e:
        print(f"벡터 데이터베이스 테스트 실패: {e}")
        return False

if __name__ == "__main__":
    test_vector_db()
