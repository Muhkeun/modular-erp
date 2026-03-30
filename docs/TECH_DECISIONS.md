# Technology Decisions & Dependency Management

> 모든 외부 의존성(라이브러리, 서비스, API)은 정기적으로 평가하고, 더 나은 대안이 있으면 교체합니다.
> 새 의존성 도입 시 반드시 이 문서의 평가 프로세스를 거칩니다.

## Evaluation Process (의존성 평가 프로세스)

새 라이브러리/서비스 도입 또는 기존 것 교체 시 아래 기준으로 평가:

| 기준 | 질문 | 가중치 |
|------|------|--------|
| **벤더 독립성** | 특정 벤더에 lock-in 되는가? 대체 가능한가? | 높음 |
| **공공/오픈소스 우선** | 정부 공공 API나 오픈소스 대안이 있는가? | 높음 |
| **라이선스** | 상용 SaaS에서 사용 가능한 라이선스인가? | 필수 |
| **유지보수** | 활발히 유지보수되는가? 최근 릴리스는? | 높음 |
| **커뮤니티** | 충분한 사용자/문서/스택오버플로우가 있는가? | 중간 |
| **성능** | 우리 규모에서 병목이 되지 않는가? | 중간 |
| **셀프호스팅** | 필요 시 자체 인프라에서 운영 가능한가? | 중간 |
| **비용** | 사용량 기반 비용이 SaaS 마진을 훼손하지 않는가? | 높음 |
| **보안** | 알려진 취약점이 없는가? 데이터가 외부로 나가는가? | 필수 |

### 평가 결과 기록 형식

```
### [카테고리] 현재선택 vs 대안
- **현재**: 라이브러리명 vX.Y.Z
- **대안**: 대안1, 대안2
- **결정**: 현재 유지 / 교체 예정
- **사유**: ...
- **재평가 시점**: YYYY-MM
```

---

## Current Dependency Inventory

### Frontend (Production)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| react | ^19.2.4 | UI 프레임워크 | - | 메이저 릴리스 시 |
| react-router-dom | ^7.13.1 | 라우팅 | TanStack Router | 2026-06 |
| axios | ^1.13.6 | HTTP 클라이언트 | ky, fetch wrapper | 2026-06 |
| @tanstack/react-query | ^5.90.21 | 서버 상태 관리 | SWR | 현재 유지 |
| zustand | ^5.0.12 | 클라이언트 상태 | Jotai, Valtio | 현재 유지 |
| ag-grid-react | ^35.1.0 | 데이터 그리드 | TanStack Table | 현재 유지 (Enterprise 기능 필요) |
| i18next | ^25.8.18 | 다국어 | - | 현재 유지 |
| tailwindcss | ^3.4.19 | CSS | Tailwind v4 | 2026-06 (v4 안정화 후) |
| lucide-react | ^0.577.0 | 아이콘 | - | 현재 유지 |
| class-variance-authority | ^0.7.1 | 컴포넌트 변형 | - | 현재 유지 |

### Frontend (Dev)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| vite | ^8.0.0 | 빌드 | - | 현재 유지 |
| vitest | ^3.2.0 | 유닛 테스트 | - | 현재 유지 |
| @playwright/test | ^1.50.0 | E2E 테스트 | Cypress | 현재 유지 |
| typescript | ~5.9.3 | 타입 시스템 | - | 마이너 추적 |

### Backend (Core)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| kotlin | 1.9.25 | 언어 | Kotlin 2.x | 2026-06 (2.x 안정화 후) |
| spring-boot | 3.4.3 | 프레임워크 | - | 마이너 추적 |
| spring-data-jpa | (boot managed) | ORM | - | 현재 유지 |
| spring-security | (boot managed) | 인증/인가 | - | 현재 유지 |
| postgresql | (boot managed) | DB | - | 현재 유지 |
| flyway | 10.22.0 | 마이그레이션 | Liquibase | 현재 유지 |
| jjwt | 0.12.6 | JWT | - | 현재 유지 |
| jackson-module-kotlin | (boot managed) | 직렬화 | kotlinx.serialization | 2026-09 |

### Backend (문서 생성)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| itext-core | 8.0.5 | PDF 생성 | OpenPDF (LGPL) | 2026-06 (라이선스 검토) |
| apache-poi | 5.3.0 | Excel 생성 | FastExcel | 2026-06 |
| springdoc-openapi | 2.8.5 | API 문서 | - | 현재 유지 |

### Backend (AI)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| langchain4j-anthropic | 1.0.0-beta3 | LLM 연동 | Spring AI | 2026-06 (정식 릴리스 후) |
| langchain4j-pgvector | 1.0.0-beta3 | 벡터 검색 | Spring AI pgvector | 동일 |

### Backend (테스트)

| 라이브러리 | 버전 | 용도 | 대안 | 재평가 |
|-----------|------|------|------|--------|
| testcontainers | 1.20.4 | 통합 테스트 | - | 현재 유지 |
| mockito-kotlin | 5.2.1 | 모킹 | MockK | 2026-06 |

---

## External Services

### [인증] SSO Providers
- **현재**: Google OAuth2, Azure AD, Okta, SAML 2.0 (모두 disabled)
- **결정**: 멀티 프로바이더 지원 유지. Spring Security OAuth2 Client 기반
- **원칙**: 특정 IdP에 lock-in 금지. 표준 프로토콜(OIDC, SAML) 기반

### [AI] LLM Provider
- **현재**: Anthropic Claude (claude-sonnet-4-20250514)
- **대안**: OpenAI, Google Gemini, 로컬 모델 (Ollama)
- **결정**: LangChain4j 추상화 레이어로 프로바이더 교체 가능하게 유지
- **원칙**: AI 기능은 optional. LLM 없이도 핵심 ERP 기능 동작해야 함

### [인프라] Cloud
- **현재**: AWS (ECR, EC2, Secrets Manager) - ap-northeast-2
- **결정**: Docker 기반이므로 타 클라우드 이동 가능. IaC 추가 필요
- **원칙**: 클라우드 전용 서비스 최소 사용. 컨테이너 기반 이식성 유지

### [주소검색] Address Lookup ⭐ 신규 도입 필요
- **결정**: 프로바이더 독립 아키텍처 + 국가별 어댑터 패턴
- **한국**: **juso.go.kr (도로명주소 API)** - 행정안전부 무료 공공 API
  - 도로명/지번 변환, 우편번호, 건물코드, 영문주소
  - 등록: business.juso.go.kr
  - 비용: 무료
  - 데이터: 정부 공식 (가장 정확)
- **국제**: **libpostal** (오픈소스, 셀프호스팅) + **Nominatim** (OSM 기반)
  - libpostal: 230개국, 100개 언어, 99.45% 파싱 정확도
  - Nominatim: 지오코딩/역지오코딩
- **❌ 미채택**: Kakao 주소 API (벤더 종속), Google Places (한국 미지원), Naver (벤더 종속)
- **구현 패턴**:
  ```
  AddressSearchPort (인터페이스)
  ├── JusoGoKrAdapter (한국)
  ├── LibpostalAdapter (국제 파싱)
  ├── NominatimAdapter (국제 지오코딩)
  └── (향후) CountrySpecificAdapter
  ```

---

## Quarterly Review Checklist

매 분기 아래 항목 점검:

- [ ] npm audit / gradle dependencyCheck 실행
- [ ] 메이저 버전 업데이트 가능 여부
- [ ] 사용 중단(deprecated) 라이브러리 확인
- [ ] 라이선스 변경 확인 (특히 OSS → 상용 전환 사례)
- [ ] 더 나은 대안 등장 여부
- [ ] 사용하지 않는 의존성 제거

## Changelog
- 2026-03-20: Initial dependency inventory and evaluation process created
