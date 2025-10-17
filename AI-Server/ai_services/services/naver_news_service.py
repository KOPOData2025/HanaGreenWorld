"""
네이버 뉴스 API 서비스
실제 환경/금융 뉴스 수집
"""
import requests
import logging
from typing import List, Dict, Any
from datetime import datetime, timedelta
import time
import os

logger = logging.getLogger(__name__)

class NaverNewsService:
    def __init__(self):
        self.client_id = os.getenv('NAVER_CLIENT_ID')
        self.client_secret = os.getenv('NAVER_CLIENT_SECRET')
        
        if not self.client_id or not self.client_secret:
            raise ValueError("네이버 API 키가 설정되지 않았습니다. NAVER_CLIENT_ID, NAVER_CLIENT_SECRET 환경변수를 설정해주세요.")
        
        self.headers = {
            'X-Naver-Client-Id': self.client_id,
            'X-Naver-Client-Secret': self.client_secret
        }
        
        self.environment_keywords = [
            '환경', '친환경', '탄소중립', '기후변화', '재생에너지', 
            '그린뉴딜', '환경보호', '대기질', '수질', '폐기물',
            '생물다양성', '환경정책', '환경부', '녹색경제'
        ]
        
        self.finance_keywords = [
            'ESG', '그린본드', '친환경금융', '지속가능금융', '그린펀드',
            '탄소배출권', 'K-ETS', '탄소세', '그린카드', '친환경투자',
            'ESG투자', '그린뱅킹', '환경금융', '녹색금융'
        ]
    
    def search_news(self, keyword: str, display: int = 100, start: int = 1, sort: str = 'date') -> List[Dict[str, Any]]:
        """
        네이버 뉴스 API로 뉴스 검색
        """
        try:
            url = "https://openapi.naver.com/v1/search/news.json"
            params = {
                'query': keyword,
                'display': display,
                'start': start,
                'sort': sort
            }
            
            response = requests.get(url, headers=self.headers, params=params)
            response.raise_for_status()
            
            data = response.json()
            articles = []
            
            for item in data.get('items', []):
                # HTML 태그 제거
                title = self._clean_html(item.get('title', ''))
                description = self._clean_html(item.get('description', ''))
                
                article = {
                    'title': title,
                    'content': description,
                    'date': item.get('pubDate', ''),
                    'url': item.get('link', ''),
                    'source': '네이버뉴스'
                }
                articles.append(article)
            
            logger.info(f"'{keyword}' 키워드로 {len(articles)}개 뉴스 수집 완료")
            return articles
            
        except Exception as e:
            logger.error(f"네이버 뉴스 검색 실패 - 키워드: {keyword}, 오류: {e}")
            return []
    
    def _clean_html(self, text: str) -> str:
        """
        HTML 태그 제거
        """
        import re
        # HTML 태그 제거
        text = re.sub(r'<[^>]+>', '', text)
        # 특수 문자 제거
        text = text.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&')
        text = text.replace('&quot;', '"').replace('&#39;', "'")
        return text.strip()
    
    def collect_environment_news(self) -> List[Dict[str, Any]]:
        """
        환경 관련 뉴스 수집 (최근 1년, 효율적 수집)
        """
        all_articles = []
        
        for keyword in self.environment_keywords:
            try:
                # 키워드당 100건씩 수집 (네이버 API 최대 제한)
                articles = self.search_news(keyword, display=100, sort='date')
                all_articles.extend(articles)
                
                # 추가로 최신순이 아닌 관련도순으로도 수집
                articles_relevance = self.search_news(keyword, display=100, sort='sim')
                all_articles.extend(articles_relevance)
                
                # API 호출 제한을 위한 대기 (0.2초)
                time.sleep(0.2)
                
            except Exception as e:
                logger.error(f"환경 뉴스 수집 실패 - 키워드: {keyword}, 오류: {e}")
                continue
        
        # 중복 제거
        unique_articles = self._remove_duplicates(all_articles)
        logger.info(f"환경 관련 뉴스 {len(unique_articles)}개 수집 완료")
        
        return unique_articles
    
    def collect_finance_news(self) -> List[Dict[str, Any]]:
        """
        금융 관련 뉴스 수집
        """
        logger.info("💰 금융 관련 뉴스 수집 시작...")
        all_articles = []
        
        for keyword in self.finance_keywords:
            try:
                # 키워드당 100건씩 수집 (네이버 API 최대 제한)
                articles = self.search_news(keyword, display=100, sort='date')
                all_articles.extend(articles)
                
                # 추가로 최신순이 아닌 관련도순으로도 수집
                articles_relevance = self.search_news(keyword, display=100, sort='sim')
                all_articles.extend(articles_relevance)
                
                # API 호출 제한을 위한 대기 (0.2초)
                time.sleep(0.2)
                
            except Exception as e:
                logger.error(f"금융 뉴스 수집 실패 - 키워드: {keyword}, 오류: {e}")
                continue
        
        # 중복 제거
        unique_articles = self._remove_duplicates(all_articles)
        logger.info(f"금융 관련 뉴스 {len(unique_articles)}개 수집 완료")
        
        return unique_articles
    
    def collect_all_news(self) -> List[Dict[str, Any]]:
        """
        모든 뉴스 수집
        """
        logger.info("📰 네이버 뉴스 API로 뉴스 수집 시작...")
        
        # 환경 뉴스 수집
        env_articles = self.collect_environment_news()
        
        # 금융 뉴스 수집
        finance_articles = self.collect_finance_news()
        
        # 전체 뉴스 합치기
        all_articles = env_articles + finance_articles
        
        # 최종 중복 제거
        unique_articles = self._remove_duplicates(all_articles)
        
        logger.info(f"총 {len(unique_articles)}개의 고유 뉴스 수집 완료")
        logger.info(f"- 환경 관련: {len(env_articles)}개")
        logger.info(f"- 금융 관련: {len(finance_articles)}개")
        
        return unique_articles
    
    def _remove_duplicates(self, articles: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        중복 뉴스 제거
        """
        seen_titles = set()
        unique_articles = []
        
        for article in articles:
            title = article.get('title', '')
            if title and title not in seen_titles:
                seen_titles.add(title)
                unique_articles.append(article)
        
        return unique_articles
    
    def save_articles_to_file(self, articles: List[Dict[str, Any]], filename: str = 'naver_news_articles.json'):
        """
        수집된 뉴스를 파일로 저장
        """
        try:
            import json
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(articles, f, ensure_ascii=False, indent=2)
            
            logger.info(f"뉴스 {len(articles)}개를 {filename}에 저장 완료")
            return True
            
        except Exception as e:
            logger.error(f"파일 저장 실패: {e}")
            return False

# 테스트용 함수
def test_naver_news():
    """
    네이버 뉴스 API 테스트
    """
    try:
        # 서비스 초기화
        news_service = NaverNewsService()
        
        # 뉴스 수집 테스트
        articles = news_service.collect_all_news()
        
        if articles:
            # 첫 번째 뉴스 출력
            first_article = articles[0]
        
            # 파일 저장
            news_service.save_articles_to_file(articles)
            
        else:
            print("❌ 뉴스 수집 실패")
        
        return True
        
    except Exception as e:
        print(f"❌ 네이버 뉴스 API 테스트 실패: {e}")
        return False

if __name__ == "__main__":
    test_naver_news()
