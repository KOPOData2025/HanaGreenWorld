#!/bin/bash

# AI 서버 실행 스크립트

echo "🤖 AI 서버를 시작합니다..."

# 가상환경 활성화
source venv/bin/activate

# 로그 디렉토리 생성
mkdir -p logs

# 환경변수 파일 확인
if [ ! -f .env ]; then
    echo "⚠️  .env 파일이 없습니다. env_example.txt를 참고하여 .env 파일을 생성해주세요."
    echo "📋 필요한 환경변수:"
    echo "   - GEMINI_API_KEY"
    echo "   - OPENAI_API_KEY"
    echo "   - SECRET_KEY"
    exit 1
fi

# Django 마이그레이션 실행
echo "📦 데이터베이스 마이그레이션을 실행합니다..."
python manage.py migrate

# 서버 실행
echo "🚀 AI 서버를 실행합니다..."
echo "📍 서버 주소: http://localhost:8083"
echo "📊 API 문서: http://localhost:8083/"
echo "🏥 헬스체크: http://localhost:8083/health/"

python manage.py runserver 0.0.0.0:8083
