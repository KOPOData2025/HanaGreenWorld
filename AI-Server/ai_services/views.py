"""
AI 서비스 뷰
"""
from rest_framework.decorators import api_view, parser_classes
from rest_framework.parsers import MultiPartParser, JSONParser
from rest_framework.response import Response
from rest_framework import status
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import logging

from .services.gemini_service import GeminiService
from .services.gpt_service import GPTService
from .services.rag_quiz_service import RAGQuizService
from .services.vector_db_service import VectorDBService
from .services.langchain_rag_service import LangChainRAGService
from .services.naver_news_service import NaverNewsService

logger = logging.getLogger(__name__)

@api_view(['GET'])
def health_check(request):
    """AI 서버 상태 확인"""
    return JsonResponse({
        'status': 'UP',
        'service': 'ai-server',
        'version': '1.0.0',
        'available_services': ['gemini', 'gpt']
    })

@api_view(['POST'])
@parser_classes([JSONParser])
def generate_text(request):
    """텍스트 생성 (Gemini 또는 GPT)"""
    try:
        data = request.data
        prompt = data.get('prompt')
        model = data.get('model', 'gemini')  # 'gemini' 또는 'gpt'
        
        if not prompt:
            return Response({
                'success': False,
                'error': 'prompt는 필수입니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        if model == 'gemini':
            service = GeminiService()
            result = service.generate_text(prompt)
        elif model == 'gpt':
            service = GPTService()
            result = service.generate_text(prompt)
        else:
            return Response({
                'success': False,
                'error': '지원하지 않는 모델입니다. (gemini, gpt)'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"텍스트 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([MultiPartParser])
def analyze_image(request):
    """이미지 분석 (Gemini 또는 GPT)"""
    try:
        model = request.data.get('model', 'gemini')
        prompt = request.data.get('prompt', '이 이미지를 분석해주세요.')
        
        if 'image' not in request.FILES:
            return Response({
                'success': False,
                'error': '이미지 파일이 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        image_file = request.FILES['image']
        image_data = image_file.read()
        
        if model == 'gemini':
            service = GeminiService()
            result = service.analyze_image(image_data, prompt)
        elif model == 'gpt':
            service = GPTService()
            result = service.analyze_image(image_data, prompt)
        else:
            return Response({
                'success': False,
                'error': '지원하지 않는 모델입니다. (gemini, gpt)'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"이미지 분석 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def generate_quiz_question(request):
    """퀴즈 문제 생성 (Gemini)"""
    try:
        data = request.data
        
        service = GeminiService()
        result = service.generate_quiz_question()
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"퀴즈 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def generate_quiz_question_openai(request):
    """퀴즈 문제 생성 (OpenAI GPT-4)"""
    try:
        data = request.data
        
        service = GPTService()
        result = service.generate_quiz_question()
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"OpenAI 퀴즈 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def compare_quiz_generation(request):
    """두 모델로 퀴즈 생성 비교"""
    try:
        import time
        from concurrent.futures import ThreadPoolExecutor
        
        def generate_gemini_quiz():
            start_time = time.time()
            service = GeminiService()
            result = service.generate_quiz_question()
            end_time = time.time()
            result['generation_time'] = end_time - start_time
            result['model'] = 'gemini-2.0-flash'
            return result
        
        def generate_openai_quiz():
            start_time = time.time()
            service = GPTService()
            result = service.generate_quiz_question()
            end_time = time.time()
            result['generation_time'] = end_time - start_time
            result['model'] = 'gpt-4'
            return result
        
        # 병렬로 두 모델 실행
        with ThreadPoolExecutor(max_workers=2) as executor:
            gemini_future = executor.submit(generate_gemini_quiz)
            openai_future = executor.submit(generate_openai_quiz)
            
            gemini_result = gemini_future.result()
            openai_result = openai_future.result()
        
        return Response({
            'success': True,
            'comparison': {
                'gemini': gemini_result,
                'openai': openai_result,
                'timestamp': time.time()
            }
        })
        
    except Exception as e:
        logger.error(f"퀴즈 비교 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def analyze_transaction(request):
    """거래내역 친환경 태깅 분석 (Gemini)"""
    try:
        data = request.data
        transactions = data.get('transactions', [])
        
        if not transactions:
            return Response({
                'success': False,
                'error': '거래내역이 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        service = GeminiService()
        result = service.analyze_transactions(transactions)
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"거래내역 분석 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def benefit_recommendation(request):
    """혜택 추천 (AI 기반) - 서비스 비활성화됨"""
    return Response({
        'success': False,
        'error': '혜택 추천 서비스가 현재 비활성화되어 있습니다.'
    }, status=status.HTTP_503_SERVICE_UNAVAILABLE)

@api_view(['POST'])
@parser_classes([MultiPartParser])
@csrf_exempt
def verify_challenge_image(request):
    """챌린지 이미지 인증 (Gemini AI)"""
    try:
        logger.info("AI 검증 API 호출됨!")
        
        challenge_title = request.data.get('challengeTitle', '')
        challenge_code = request.data.get('challengeCode', '')
        
        logger.info(f"요청 데이터 - 제목: {challenge_title}, 코드: {challenge_code}")
        
        if not challenge_title or not challenge_code:
            logger.error("챌린지 제목 또는 코드가 누락됨")
            return Response({
                'success': False,
                'error': '챌린지 제목과 코드가 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        if 'image' not in request.FILES:
            logger.error("이미지 파일이 없음")
            return Response({
                'success': False,
                'error': '이미지 파일이 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        image_file = request.FILES['image']
        image_data = image_file.read()
        
        service = GeminiService()
        result = service.verify_challenge_image(image_data, challenge_title, challenge_code)
        
        
        return Response(result)
        
    except Exception as e:
        logger.error(f"챌린지 이미지 검증 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def generate_rag_quiz(request):
    """RAG 기반 퀴즈 생성 (최신 뉴스 활용)"""
    try:
        data = request.data
        topic = data.get('topic', '친환경 활동')
        difficulty = data.get('difficulty', 'medium')
        
        logger.info(f"🎯 RAG 퀴즈 생성 요청 - 주제: {topic}, 난이도: {difficulty}")
        
        service = RAGQuizService()
        result = service.get_quiz_with_latest_news(topic, difficulty)
        
        logger.info(f"RAG 퀴즈 생성 완료 - {len(result.get('quiz', []))}개 문제")
        
        return Response({
            'success': True,
            'data': result
        })
        
    except Exception as e:
        logger.error(f"RAG 퀴즈 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def update_news_data(request):
    """뉴스 데이터 업데이트"""
    try:
        logger.info("📰 뉴스 데이터 업데이트 요청")
        
        service = RAGQuizService()
        success = service.update_news_data()
        
        if success:
            # 업데이트 후 통계 정보 반환
            vector_db = VectorDBService()
            stats = vector_db.get_collection_stats()
            
            logger.info(f"✅ 뉴스 데이터 업데이트 완료 - 총 {stats.get('total_articles', 0)}개 기사")
            
            return Response({
                'success': True,
                'message': '뉴스 데이터 업데이트 완료',
                'stats': stats
            })
        else:
            return Response({
                'success': False,
                'error': '뉴스 데이터 업데이트 실패'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
    except Exception as e:
        logger.error(f"뉴스 데이터 업데이트 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
def get_vector_db_stats(request):
    """벡터 데이터베이스 통계 조회"""
    try:
        vector_db = VectorDBService()
        stats = vector_db.get_collection_stats()
        
        return Response({
            'success': True,
            'data': stats
        })
        
    except Exception as e:
        logger.error(f"벡터 DB 통계 조회 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def search_related_news(request):
    """관련 뉴스 검색"""
    try:
        data = request.data
        query = data.get('query', '')
        limit = data.get('limit', 5)
        
        if not query:
            return Response({
                'success': False,
                'error': '검색 쿼리가 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        service = RAGQuizService()
        results = service.search_related_news(query, limit)
        
        return Response({
            'success': True,
            'data': {
                'query': query,
                'results': results,
                'count': len(results)
            }
        })
        
    except Exception as e:
        logger.error(f"뉴스 검색 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def generate_daily_quiz(request):
    """LangChain RAG 기반 일일 퀴즈 생성 (쉬움-보통 난이도)"""
    try:
        data = request.data
        topic = data.get('topic', '친환경 활동')
        
        service = LangChainRAGService()
        result = service.generate_daily_quiz(topic)
        
        if result['success']:
            logger.info(f"일일 퀴즈 생성 완료 - {len(result.get('quiz', []))}개 문제")
        else:
            logger.error(f"일일 퀴즈 생성 실패: {result.get('error', 'Unknown error')}")
        
        return Response({
            'success': result['success'],
            'data': result
        })
        
    except Exception as e:
        logger.error(f"일일 퀴즈 생성 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def update_news_langchain(request):
    """LangChain RAG 뉴스 데이터 업데이트"""
    try:
        logger.info("📰 LangChain RAG 뉴스 데이터 업데이트 요청")
        
        # 기존 네이버 뉴스 데이터 로드
        import json
        try:
            with open('naver_news_articles.json', 'r', encoding='utf-8') as f:
                new_articles = json.load(f)
            logger.info(f"기존 네이버 뉴스 {len(new_articles)}개 로드 완료")
        except FileNotFoundError:
            logger.error("naver_news_articles.json 파일을 찾을 수 없습니다.")
            return Response({
                'success': False,
                'error': '네이버 뉴스 데이터 파일을 찾을 수 없습니다.'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        if not new_articles:
            return Response({
                'success': False,
                'error': '뉴스 데이터가 없습니다.'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        # LangChain RAG 서비스에 벡터화만 수행
        rag_service = LangChainRAGService()
        success = rag_service.add_news_to_vectorstore(new_articles)
        
        if success:
            # 업데이트 후 통계 정보 반환
            stats = rag_service.get_vectorstore_stats()
            
            logger.info(f"LangChain RAG 뉴스 데이터 업데이트 완료 - 총 {stats.get('total_documents', 0)}개 문서")
            
            return Response({
                'success': True,
                'message': 'LangChain RAG 뉴스 데이터 업데이트 완료',
                'stats': stats
            })
        else:
            return Response({
                'success': False,
                'error': 'LangChain RAG 뉴스 데이터 업데이트 실패'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
    except Exception as e:
        logger.error(f"LangChain RAG 뉴스 데이터 업데이트 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
def get_langchain_stats(request):
    """LangChain RAG 벡터 스토어 통계 조회"""
    try:
        rag_service = LangChainRAGService()
        stats = rag_service.get_vectorstore_stats()
        
        return Response({
            'success': True,
            'data': stats
        })
        
    except Exception as e:
        logger.error(f"LangChain RAG 통계 조회 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def search_langchain_documents(request):
    """LangChain RAG 문서 검색"""
    try:
        data = request.data
        query = data.get('query', '')
        limit = data.get('limit', 5)
        
        if not query:
            return Response({
                'success': False,
                'error': '검색 쿼리가 필요합니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        rag_service = LangChainRAGService()
        results = rag_service.search_similar_documents(query, k=limit)
        
        return Response({
            'success': True,
            'data': {
                'query': query,
                'results': results,
                'count': len(results)
            }
        })
        
    except Exception as e:
        logger.error(f"LangChain RAG 문서 검색 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def collect_naver_news(request):
    """네이버 뉴스 API로 뉴스 수집"""
    try:
        logger.info("📰 네이버 뉴스 API 뉴스 수집 요청")
        
        news_service = NaverNewsService()
        articles = news_service.collect_all_news()
        
        if articles:
            # 파일로 저장
            news_service.save_articles_to_file(articles)
            
            logger.info(f"네이버 뉴스 수집 완료 - {len(articles)}개")
            
            return Response({
                'success': True,
                'message': f'네이버 뉴스 {len(articles)}개 수집 완료',
                'data': {
                    'total_articles': len(articles),
                    'saved_to_file': 'naver_news_articles.json'
                }
            })
        else:
            return Response({
                'success': False,
                'error': '뉴스를 수집할 수 없습니다.'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
    except Exception as e:
        logger.error(f"네이버 뉴스 수집 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@parser_classes([JSONParser])
@csrf_exempt
def update_news_with_naver(request):
    """네이버 뉴스로 RAG 시스템 업데이트"""
    try:
        logger.info("네이버 뉴스로 RAG 시스템 업데이트 요청")
        
        # 1. 기존 네이버 뉴스 데이터 로드
        import json
        try:
            with open('naver_news_articles.json', 'r', encoding='utf-8') as f:
                articles = json.load(f)
            logger.info(f"기존 네이버 뉴스 {len(articles)}개 로드 완료")
        except FileNotFoundError:
            logger.error("naver_news_articles.json 파일을 찾을 수 없습니다.")
            return Response({
                'success': False,
                'error': '네이버 뉴스 데이터 파일을 찾을 수 없습니다.'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        if not articles:
            return Response({
                'success': False,
                'error': '뉴스 데이터가 없습니다.'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        # 2. 벡터 DB에 추가
        vector_db = VectorDBService()
        success = vector_db.add_articles_to_vector_db(articles)
        
        if success:
            # 통계 정보 반환
            stats = vector_db.get_collection_stats()
            
            logger.info(f"네이버 뉴스 RAG 업데이트 완료 - 총 {stats.get('total_articles', 0)}개 문서")
            
            return Response({
                'success': True,
                'message': '네이버 뉴스로 RAG 시스템 업데이트 완료',
                'data': {
                    'new_articles': len(articles),
                    'total_articles': stats.get('total_articles', 0),
                    'source_distribution': stats.get('source_distribution', {})
                }
            })
        else:
            return Response({
                'success': False,
                'error': '벡터 데이터베이스 업데이트 실패'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
    except Exception as e:
        logger.error(f"네이버 뉴스 RAG 업데이트 오류: {str(e)}")
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)