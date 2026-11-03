# LLM RAG Portfolio

업로드한 PDF·TXT 문서를 근거로 질문에 답하는 RAG(Retrieval-Augmented Generation) 서비스입니다.
문서 업로드부터 텍스트 추출, 청크 분할, 임베딩, 벡터 검색, LLM 답변 생성까지 단계별로 직접 구현하고 있습니다.

## 진행 단계

| 단계 | 기능 | 상태 |
| --- | --- | --- |
| 1 | PDF·TXT 업로드 (확장자·크기 검증, UUID 파일명 저장) | 완료 |
| 2 | 페이지별 텍스트 추출 및 Oracle 저장 | 코드 작성 완료, Oracle 연동 확인 중 |
| 3 | 텍스트 청크 분할 | 예정 |
| 4 | 임베딩 생성 | 예정 |
| 5 | 벡터 저장 및 유사도 검색 | 예정 |
| 6 | LLM 연동 (검색 결과를 근거로 답변 생성) | 예정 |
| 7 | 출처 표시 및 오류 처리 | 예정 |

## 기술 스택

- Java 21, Spring Boot 4.0 (Web MVC, JDBC)
- Oracle Database (`ojdbc11`, `NamedParameterJdbcTemplate`)
- Apache PDFBox 3.0 (PDF 텍스트 추출)
- Gradle

## 문서 처리 흐름

```
업로드 → 검증 → 디스크 저장(uploads/) → documents 행 등록(UPLOADED)
       → 페이지별 텍스트 추출 → document_pages 저장 + EXTRACTED
                            └ 실패 시 FAILED + 실패 사유 기록
```

- 사용자가 보낸 파일명은 저장 경로에 쓰지 않고, UUID 기반 이름으로 저장합니다.
- DB 등록에 실패하면 저장한 파일을 지워 고아 파일이 남지 않게 합니다.
- 페이지 저장과 상태 변경은 한 트랜잭션으로 묶습니다.
- 추출 텍스트를 페이지 단위로 보관하므로, 청크 분할 방식을 바꿔도 PDF를 다시 읽지 않아도 됩니다.

## 실행 방법

### 1. Oracle 준비

앱 계정(기본값 `rag`)으로 접속해 [schema-oracle.sql](src/main/resources/db/schema-oracle.sql)을 실행합니다.

### 2. 접속 정보 설정

비밀번호는 코드에 넣지 않고 환경변수로 넘깁니다.

| 환경변수 | 기본값 |
| --- | --- |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `DB_USERNAME` | `rag` |
| `DB_PASSWORD` | (필수) |

```powershell
$env:DB_PASSWORD = "비밀번호"
.\gradlew.bat bootRun
```

브라우저에서 http://localhost:8080/ 에 접속하면 서버 상태를 확인할 수 있습니다. 업로드는 아래 API를 curl 등으로 호출합니다.

### 3. 빌드·테스트

```powershell
.\gradlew.bat clean build
```

`LlmRagPortfolioApplicationTests`는 스프링 컨텍스트 전체를 띄우므로 Oracle이 실행 중이어야 통과합니다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/status` | 서버 상태 확인 |
| POST | `/api/documents/upload` | 파일 업로드 + 텍스트 추출 (`multipart/form-data`, 필드명 `file`, 최대 20MB) |
| GET | `/api/documents` | 문서 목록 (최근 업로드 순) |
| GET | `/api/documents/{id}` | 문서 한 건 |
| GET | `/api/documents/{id}/pages` | 페이지 목록과 앞부분 미리보기 |
| GET | `/api/documents/{id}/pages/{pageNumber}` | 한 페이지의 추출 텍스트 전체 |

```bash
curl -F "file=@sample.pdf" http://localhost:8080/api/documents/upload
```

## 프로젝트 구조

```
src/main/java/com/portfolio/llmrag
├── controller   REST API, 공통 예외 처리
├── service      업로드·저장 흐름 (DocumentService, DocumentStorageService)
│   └── extract  PDF·TXT 텍스트 추출, 텍스트 정리
├── repository   Oracle 접근 (documents, document_pages)
├── domain       테이블 행 모델, 문서 상태
├── dto          API 응답 형식
└── exception    도메인 예외
src/main/resources
├── db/schema-oracle.sql
└── static/index.html
```
