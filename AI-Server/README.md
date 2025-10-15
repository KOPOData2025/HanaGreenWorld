# AI 서버 (Python Django)

## 🤖 개요
하나그린월드 프로젝트의 AI 서버로, Gemini API와 GPT API를 통합하여 친환경 관련 AI 서비스를 제공합니다.

## 🚀 주요 기능
- **텍스트 생성**: Gemini/GPT를 통한 텍스트 생성
- **이미지 분석**: Gemini Vision/GPT-4 Vision을 통한 이미지 분석
- **퀴즈 생성**: 친환경 퀴즈 문제 자동 생성

## 📋 요구사항
- Python 3.9+
- Django 4.2.7
- Gemini API Key
- OpenAI API Key

## 🛠️ 설치 및 실행

### 1. 의존성 설치
```bash
# 가상환경 생성 (이미 생성됨)
python3 -m venv venv

# 가상환경 활성화
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt
```

### 2. 환경변수 설정
```bash
# .env 파일 생성 (env_example.txt 참고)
cp env_example.txt .env

# .env 파일 편집
nano .env
```

필수 환경변수:
```env
DEBUG=True
SECRET_KEY=your-secret-key-here
GEMINI_API_KEY=your-gemini-api-key-here
OPENAI_API_KEY=your-openai-api-key-here
```

### 3. 서버 실행
```bash
# 실행 스크립트 사용
./run_server.sh

# 또는 직접 실행
source venv/bin/activate
python manage.py migrate
python manage.py runserver 0.0.0.0:8083
```

## 📡 API 엔드포인트

### 기본 서비스
- `GET /health/` - 서버 상태 확인
- `POST /api/generate-text/` - 텍스트 생성
- `POST /api/analyze-image/` - 이미지 분석

### 친환경 서비스
- `POST /api/eco/quiz/` - 퀴즈 생성

## 🔗 hanagreenworld 연동

### 기존 변경사항
1. **QuizGeneratorService**: Gemini API 직접 호출 → AI 서버 호출로 변경
2. **AIService**: 새로운 AI 서버 통신 서비스 추가
3. **설정**: `application-setting.yml`에 AI 서버 URL 추가

### 연동 예시
```java
// 기존: Gemini API 직접 호출
@Value("${gemini.api.key}")
private String apiKey;

// 변경: AI 서버 호출
@Value("${ai.server.url}")
private String aiServerUrl;
```

## 🧪 테스트

### 헬스체크
```bash
curl http://localhost:8083/health/
```

### 퀴즈 생성 테스트
```bash
curl -X POST http://localhost:8083/eco/quiz/ \
  -H "Content-Type: application/json" \
  -d '{"category": "환경보호", "difficulty": "중급"}'
```

### 친환경 추천 테스트
```bash
curl -X POST http://localhost:8083/eco/recommendation/ \
  -H "Content-Type: application/json" \
  -d '{"user_data": {"level": "초급", "total_points": 100}}'
```

## 📊 서버 정보
- **포트**: 8083
- **프레임워크**: Django 4.2.7
- **API**: Django REST Framework
- **CORS**: django-cors-headers
- **로깅**: Django 기본 로깅

## 🔧 개발 정보
- **개발자**: AI 서버 개발팀
- **버전**: 1.0.0
- **라이선스**: MIT

## 📝 로그
서버 로그는 `logs/django.log` 파일에 저장됩니다.

## 🚨 문제 해결
1. **API 키 오류**: .env 파일의 API 키 확인
2. **포트 충돌**: 8083 포트가 사용 중인지 확인
3. **의존성 오류**: `pip install -r requirements.txt` 재실행
4. **가상환경 오류**: `source venv/bin/activate` 확인
