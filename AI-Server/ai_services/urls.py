"""
AI 서비스 URL 설정
"""
from django.urls import path
from . import views

urlpatterns = [
    # 헬스체크
    path('health/', views.health_check, name='health_check'),
    
    # 기본 AI 서비스
    path('generate-text/', views.generate_text, name='generate_text'),
    path('analyze-image/', views.analyze_image, name='analyze_image'),
    
    # 친환경 관련 AI 서비스
    path('eco/quiz/', views.generate_quiz_question, name='generate_quiz'),
    path('eco/quiz/openai/', views.generate_quiz_question_openai, name='generate_quiz_openai'),
    path('eco/quiz/compare/', views.compare_quiz_generation, name='compare_quiz_generation'),
    path('eco/analyze-transaction/', views.analyze_transaction, name='analyze_transaction'),
    path('eco/verify-challenge-image/', views.verify_challenge_image, name='verify_challenge_image'),
    
    # RAG 기반 퀴즈 서비스
    path('eco/quiz/rag/', views.generate_rag_quiz, name='generate_rag_quiz'),
    path('eco/news/update/', views.update_news_data, name='update_news_data'),
    path('eco/news/search/', views.search_related_news, name='search_related_news'),
    path('eco/vector-db/stats/', views.get_vector_db_stats, name='get_vector_db_stats'),
    
    # LangChain RAG 기반 일일 퀴즈 서비스
    path('eco/quiz/daily/', views.generate_daily_quiz, name='generate_daily_quiz'),
    path('eco/news/update-langchain/', views.update_news_langchain, name='update_news_langchain'),
    path('eco/langchain/stats/', views.get_langchain_stats, name='get_langchain_stats'),
    path('eco/langchain/search/', views.search_langchain_documents, name='search_langchain_documents'),
    
    # 네이버 뉴스 API 서비스
    path('eco/news/naver/', views.collect_naver_news, name='collect_naver_news'),
    path('eco/news/update-naver/', views.update_news_with_naver, name='update_news_with_naver'),

]
