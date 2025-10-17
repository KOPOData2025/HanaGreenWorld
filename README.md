# 하나그린세상 - 친환경과 금융을 잇는 고객 중심 녹색금융 플랫폼
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 42 12" src="https://github.com/user-attachments/assets/93d75123-450e-42e6-8bcc-6436f56edd23" />

<br><br>

## 목차 (Table of Contents)
1. [기획 배경](#1-기획-배경)
2. [프로젝트 소개 및 목표](#2-프로젝트-소개-및-목표)
3. [서비스 아키텍처](#3-서비스-아키텍처)
4. [시스템 아키텍처](#4-시스템-아키텍처)
5. [활용 기술](#5-활용-기술)
6. [특화 기술](#6-특화-기술)
7. [개발 정보](#7-개발-정보)

<br>

## 1. 기획 배경

<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 41 40" src="https://github.com/user-attachments/assets/ca0c3e64-59c6-4bd7-a33a-3f331e88f4fa" />

- 사회 전반의 환경 의식이 높아지며 친환경 소비와 ESG 경영이 확산되는 추세
- 하나금융그룹도 2030년까지 녹색금융 및 ESG 투자 35조 원 달성을 목표로 노력 중
- 그러나 현재는 기업 중심의 녹색금융이 주를 이루고 있어, 고객이 직접 참여할 수 있는 친환경 금융 플랫폼의 필요성 인식
- 이에 따라 친환경 활동과 금융 혜택을 연결하는 플랫폼 <strong>‘하나그린세상’</strong>을 기획

<br>

## 2. 프로젝트 소개 및 목표
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 43 13" src="https://github.com/user-attachments/assets/357487ba-d9d9-4c16-9c74-47af66877afb" />

- 고객이 일상 속에서 친환경 활동을 실천하고, 그 결과를 금융 혜택으로 환원받는 서비스 제공  
- 챌린지·걷기·환경 퀴즈 등 참여형 친환경 활동을 통해 자체 친환경 포인트(원큐씨앗)를 적립할 수 있는 시스템 구현  
- 하나금융그룹의 ESG 이미지를 강화하고, 고객 중심 녹색금융 생태계 구축에 기여  

<br>

## 3. 서비스 아키텍처
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 34 04" src="https://github.com/user-attachments/assets/153b2928-7b58-4bd2-b0d4-cc5b47e070db" />

- <strong>친환경 활동 & 챌린지:</strong> 환경 퀴즈, 걷기, 전자확인증 발급, 챌린지 등을 통해 자체 친환경 포인트(원큐씨앗)를 적립합니다.
- <strong>팀 기반 소셜 기능:</strong> 팀 결성을 통해 팀 챌린지 및 점수 경쟁이 가능하며, 팀원들과 실시간 채팅이 가능합니다.
- <strong>원큐씨앗 포인트 시스템:</strong> 친환경 활동을 통해 얻은 친환경 포인트를 하나머니로 전환할 수 있습니다.
- <strong>친환경 금융상품 조회 및 가입:</strong> 하나금융그룹의 친환경 상품(적금, 대출, 카드)를 조회 및 가입할 수 있으며, 누적 포인트에 혜택이 부여됩니다.
- <strong>매달 친환경 리포트 제공:</strong> 지난달의 친환경 활동으로 얻은 금융 혜택과 환경적 가치를 함께 보여줍니다.

<br>

## 4. 시스템 아키텍처
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 35 25" src="https://github.com/user-attachments/assets/3f61188f-7bf4-4400-8944-7bbd7452a0ac" />

<br><br>

## 5. 활용 기술
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 37 40" src="https://github.com/user-attachments/assets/0c98c558-94a6-494b-9c49-7334e806f13b" />

- <strong>하나그린세상 프론트:</strong> '하나원큐 앱' 속 서비스이기 때문에 React native, expo를 통해 개발
- <strong>하나그린세상 백엔드:</strong> java, spring 활용, websocket/stomp를 통해 채팅 구현
- <strong>하나카드 백엔드:</strong> java, spring 활용
- <strong>하나은행 백엔드:</strong> java, spring 활용
- <strong>AI 서버:</strong> python, django 활용

<br><br>

## 6. 특화 기술
### RAG 기반 지능형 퀴즈 생성 시스템
<img width="800" height="456" alt="스크린샷 2025-10-17 오후 5 40 24" src="https://github.com/user-attachments/assets/c02c4276-77c7-4d08-8006-d53262824b50" />

<br><br>

## 7. 개발 정보

- **개발 기간**: 2025.09.01 ~ 2025.10.24
- **개발 인원**: 1인 (풀스택 개발)
- **개발 환경**:
  - OS: macOS Sonoma 15.5
  - Backend: IntelliJ IDEA, Spring Boot, Django
  - Frontend: Visual Studio Code, React Native, Expo
  - Database: IntelliJ IDEA, Oracle SQL Developer
  - API Testing: Postman, Swagger UI

|구분|내용|비고|
|:--:|:--:|:--:|
**이름**|박지민|<img width="100" height="150" alt="image" src="https://github.com/user-attachments/assets/d14778ac-5a20-4909-a6ec-0ae19da9c9c5" />|
**연락처**|이메일|jimin1299@naver.com|
**학력**|숭실대학교 소프트웨어학부|2019.03~2024.08|
**Skill set**|Language|Java, Python, C, Typescript, JSP
||Framework & Library|Spring Boot, Django, React Native|
||Database|MySQL, Oracle, Redis|
||ETC|Git, AWS, GCP, Docker|
|**자격증**|2025.03.21 | 데이터분석준전문가(ADsP)|
|| 2024.06.21 | SQLD (SQL 개발자)|
|| 2023.09.23 | 정보처리기사 |
|**수상**|제13회 숭실 캡스톤디자인 경진대회 (동상)|숭실대학교 공학교육혁신센터(2023.09.19)|
|**교육**|42Seoul|2022.07~2024.04|
