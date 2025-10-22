#!/usr/bin/env python3
"""
실제 이미지로 다층 AI 검증 시스템 테스트

사용법:
python test_with_real_image.py <이미지_경로> <챌린지_코드>

예시:
python test_with_real_image.py test_image.jpg TUMBLER_CHALLENGE
"""

import os
import sys
import requests
import json
from pathlib import Path

def test_api_with_image(image_path: str, challenge_code: str, challenge_title: str = None):
    """실제 이미지로 API 테스트"""
    
    if not os.path.exists(image_path):
        print(f"이미지 파일이 존재하지 않습니다: {image_path}")
        return False
    
    if challenge_title is None:
        challenge_title = challenge_code.replace('_', ' ').title()
    
    # API 엔드포인트
    url = "http://localhost:8000/api/eco/verify-challenge-image/"
    
    # 요청 데이터 준비
    files = {
        'image': open(image_path, 'rb')
    }
    data = {
        'challengeTitle': challenge_title,
        'challengeCode': challenge_code
    }
    
    print(f"🔍 이미지 검증 테스트 시작...")
    print(f"   - 이미지: {image_path}")
    print(f"   - 챌린지: {challenge_title} ({challenge_code})")
    print(f"   - API: {url}")
    
    try:
        # API 호출
        response = requests.post(url, files=files, data=data)
        
        # 응답 확인
        if response.status_code == 200:
            result = response.json()
            
            print(f"\n✅ API 호출 성공!")
            print(f"📊 검증 결과:")
            print(f"   - 판정: {result.get('verification_result', 'N/A')}")
            print(f"   - 신뢰도: {result.get('confidence', 0):.3f}")
            print(f"   - 모델: {result.get('model', 'N/A')}")
            print(f"   - 설명: {result.get('explanation', 'N/A')}")
            
            # 상세 정보 출력
            if 'verification_details' in result:
                details = result['verification_details']
                print(f"\n🔍 상세 검증 정보:")
                print(f"   - Gemini 신뢰도: {details.get('gemini_confidence', 0):.3f}")
                print(f"   - CLIP 유사도: {details.get('clip_similarity', 0):.3f}")
                print(f"   - 일관성 점수: {details.get('consistency_score', 0):.3f}")
                print(f"   - 키워드 매칭: {details.get('keyword_match', False)}")
                print(f"   - CLIP 통과: {details.get('clip_passed', False)}")
                print(f"   - Self-check 일관성: {details.get('self_check_consistent', False)}")
                print(f"   - 키워드 감점: {details.get('keyword_penalty', 0):.3f}")
            
            # 감지된 객체
            if 'detected_items' in result:
                items = result.get('detected_items', [])
                if items:
                    print(f"   - 감지된 객체: {', '.join(items)}")
            
            return True
            
        else:
            print(f"❌ API 호출 실패: {response.status_code}")
            print(f"   응답: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print(f"❌ 서버 연결 실패. AI 서버가 실행 중인지 확인하세요.")
        print(f"   실행 명령: python manage.py runserver 0.0.0.0:8000")
        return False
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        return False
    finally:
        files['image'].close()

def create_test_images():
    """테스트용 이미지 생성 안내"""
    print("📸 테스트용 이미지 준비:")
    print("1. 텀블러 챌린지: 텀블러가 보이는 사진")
    print("2. 장바구니 챌린지: 에코백이나 장바구니 사진")
    print("3. 플로깅 챌린지: 쓰레기를 주우는 사진")
    print("4. 분리수거 챌린지: 분리수거하는 사진")
    print("\n💡 팁: 스마트폰으로 직접 촬영하거나 인터넷에서 다운로드")

def main():
    """메인 함수"""
    if len(sys.argv) < 3:
        print("사용법: python test_with_real_image.py <이미지_경로> <챌린지_코드>")
        print("\n챌린지 코드:")
        print("  - TUMBLER_CHALLENGE: 텀블러 챌린지")
        print("  - REUSABLE_BAG: 장바구니 챌린지")
        print("  - PLUGGING: 플로깅 챌린지")
        print("  - RECYCLE: 분리수거 챌린지")
        print("  - NO_PLASTIC: 일회용품 줄이기 챌린지")
        print("\n예시:")
        print("  python test_with_real_image.py tumbler.jpg TUMBLER_CHALLENGE")
        print("  python test_with_real_image.py bag.jpg REUSABLE_BAG")
        
        create_test_images()
        return
    
    image_path = sys.argv[1]
    challenge_code = sys.argv[2]
    challenge_title = sys.argv[3] if len(sys.argv) > 3 else None
    
    success = test_api_with_image(image_path, challenge_code, challenge_title)
    
    if success:
        print(f"\n🎉 테스트 완료! 다층 검증 시스템이 정상 작동합니다.")
    else:
        print(f"\n⚠️  테스트 실패. 로그를 확인하세요.")

if __name__ == "__main__":
    main()
