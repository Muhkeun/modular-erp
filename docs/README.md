# Modular ERP - Project Documentation

> 이 문서는 여러 LLM 세션/에이전트가 멱등하게 협업할 수 있도록 구성되었습니다.
> 각 문서는 독립적으로 읽을 수 있으며, 상태 추적 문서는 체크박스로 진행률을 관리합니다.

## Document Index

| 문서 | 용도 | 갱신 주기 |
|------|------|-----------|
| [OVERVIEW.md](./OVERVIEW.md) | 아키텍처, 기술스택, 모듈 구조 | 구조 변경 시 |
| [GAP_ANALYSIS.md](./GAP_ANALYSIS.md) | 미구현/미흡 상세 분석 | 이슈 해결 시 |
| [BUGS.md](./BUGS.md) | 확인된 버그 목록 (재현 가능) | 수정 시 |
| [ROADMAP.md](./ROADMAP.md) | 우선순위별 실행 로드맵 | 스프린트 단위 |
| [MODULE_STATUS.md](./MODULE_STATUS.md) | 모듈별 구현 상태 매트릭스 | 모듈 작업 시 |
| [API_CONTRACT.md](./API_CONTRACT.md) | 프런트/백엔드 API 계약 불일치 | 수정 시 |
| [TEST_COVERAGE.md](./TEST_COVERAGE.md) | 테스트 현황 및 커버리지 목표 | 테스트 추가 시 |
| [UX_STANDARDS.md](./UX_STANDARDS.md) | 상용 SaaS ERP UX 기준 및 GAP | UX 개선 시 |
| [TECH_DECISIONS.md](./TECH_DECISIONS.md) | 기술 의존성 평가 프로세스 + 인벤토리 | 의존성 변경 시 |

## Conventions

- **상태 표기**: `NOT_STARTED` → `IN_PROGRESS` → `DONE` → `VERIFIED`
- **우선순위**: `P0` (장애급) > `P1` (핵심) > `P2` (중요) > `P3` (개선)
- **날짜**: 절대 날짜 사용 (2026-03-20 형식)
- **체크박스**: `- [ ]` 미완료, `- [x]` 완료
- **변경 이력**: 각 문서 하단 Changelog 섹션에 기록
