# Crossfit Advisor (Backend)
이 프로젝트는 Crossfit 초심자들을 위한 보강운동 추천 및 보조를 받을 수 있는 AI Agent 애플리케이션 개발이 목적입니다.

## 기본 프로젝트 요구사항
1. 사용자는 WOD 를 사진 혹은 텍스트 기반으로 AI Agent 에게 전달할 수 있습니다.
2. AI Agent 는 WOD 를 분석하여 그날의 WOD 후에 적절한 보강운동을 설계해줍니다.
3. 사용자는 AI Agent와 대화를 통해 보강운동을 조절할 수 있습니다.

## 향후 로드맵 (업데이트 중, 변경 가능성 존재)
1. 사용자가 선호하는 보강 부위/방식을 고려하여 추천을 할 수 있도록 개인화된 서비스를 제공하고자 합니다.
2. 개인별 레코드를 관리하고, 운동능력의 향상을 시각화하는 통계 기능을 제공하고자 합니다.
3. 워치 등과 연계하여 보강 운동 세트를 카운트할 수 있는 기능을 제공하고자 합니다.

## 참조
본 프로젝트의 Frontend 소스코드는 이 링크를 참조하세요!
- [또와드 (Frontend)](https://github.com/sh827kim/crossfit-advisor-frontend)

## 프로젝트 기술 스택
- 개발 언어 : JAVA 21
- 빌드 : Gradle 9.1.0
- 프레임워크 : Spring Boot 3.5.9
  - Spring AI 1.1.2
- DB : PostgreSQL

## 실행 방법
아래 순서를 실행하기 전에, 로컬에 자신만의 PostgreSQL DB 를 설치해주세요.
- 참조 링크 : https://www.postgresql.org/download/

1. 프로젝트를 아래 명령어로 클론하세요.
```shell
git clone https://github.com/sh827kim/crossfit-advisor-backend.git
```
2. 로컬용 환경변수 구성을 위해 application-local.yaml을 만들어주세요
```yaml
spring:
  config:
    activate:
      on-profile: local
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - profile
              - email
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-nano
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
custom:
    application:
      default-login-success-url: ${FRONTEND_URL}/dashboard
      default-login-failure-url: ${FRONTEND_URL}/login/error
      default-logout-success-url: ${FRONTEND_URL}/login
      google-api-key: ${GOOGLE_API_KEY}
      origins:
        - ${FRONTEND_URL}
      jwt:
        issuer: ${JWT_ISSUER}
        audience: ${JWT_AUDIENCE}
        key-id: ${JWT_KEY_ID}
        access-token-ttl: 3600 # 60 minutes
        refresh-token-ttl: 7 # 7 days
        refresh-token-purpose: refresh
        access-token-purpose: access
        private-key: ${JWT_PRIVATE_KEY}
        public-key: ${JWT_PUBLIC_KEY}
server:
  servlet:
    session:
      cookie:
        same-site: none
        secure: true
```
3. 환경변수들을 채워넣어주세요. 각 환경변수들은 다음과 같은 의미를 가집니다.
- GOOGLE_CLIENT_ID : 구글 로그인 연동을 위한 클라이언트 ID
- GOOGLE_CLIENT_SECRET : 구글 로그인 연동을 위한 클라이언트 Secret
- OPENAI_API_KEY : OpenAI 에서 발급받은 API Key
- DATABASE_URL : 데이터베이스 접속 주소. ex) jdbc:postgresql://localhost:5432/crossfit
- DATABASE_USERNAME : 데이터베이스 계정
- DATABASE_PASSWORD : 데이터베이스 패스워드
- FRONTEND_URL : 프론트엔드 어플리케이션 주소
- GOOGLE_API_KEY : OCR API 연동을 위한 구글 API 키
- JWT_ISSUER : 토큰 발행자. ex) crossfit-demo-backend
- JWT_AUDIENCE : 토큰 활용자. ex) crossfit-demo-frontend
- JWT_KEY_ID : JWT 키 ID 값. ex) my-crossfit
- JWT_PRIVATE_KEY : JWT 토큰 생성에 활용되는 RSA pkcs8 형색의 pem 파일 내 내용
- JWT_PUBLIC_KEY : 해당 pem 파일 기반 생성된 public key 내용


JWT private key, public key는 아래 명령어를 통해 pem 파일을 생성 후 아래 예시와 같이 추가하시면 됩니다. 
- pem 파일 생성 명령어
```shell
openssl genrsa -out private_key.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private_key.pem -out private_key_pkcs8.pem # JWT_PRIVATE_KEY에 들어갈 내용
openssl rsa -in private_key.pem -pubout -out public_key.pem # JWT_PUBLIC_KEY 에 들어갈 내용
```
- 예시
```
      private-key: |
        -----BEGIN PRIVATE KEY-----
        MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCv16l9hyeNJ5Q0
        ....... 내용 생략
        CRsJRLK4AkCP1vqVUOmkbR4=
        -----END PRIVATE KEY-----
      public-key: |
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr9epfYcnjSeUNKEBvbJT
        ........ 내용 생략
        fwIDAQAB
        -----END PUBLIC KEY-----
```


4. 프로젝트를 빌드 및 실행해주세요.
```shell
./gradlew bootJar
java -jar build/libs/*.jar
```

## 소스코드 구조
(업데이트 예정)

## 의존성 업그레이드 계획
본 프로젝트는 Spring AI의 Spring boot 버전 지원 정책으로 인해 Spring Boot 3.5.9 버전을 사용하고 있습니다. 
향후 Spring AI 2.x 버전이 릴리즈되면 Spring boot 4.x 버전으로 업그레이드할 계획입니다.
