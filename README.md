# LLM RAG Portfolio

업로드한 PDF·TXT 문서를 근거로 질문에 답하는 RAG(Retrieval-Augmented Generation) 서비스입니다.
문서 업로드부터 텍스트 추출, 청크 분할, 임베딩, 벡터 검색, LLM 답변 생성까지 단계별로 직접 구현하고 있습니다.

## 진행 단계

| 단계 | 기능                                                 | 상태 |
| ---- | ---------------------------------------------------- | ---- |
| 1    | PDF·TXT 업로드 (확장자·크기 검증, UUID 파일명 저장)  | 완료 |
| 2    | 페이지별 텍스트 추출 및 Oracle 저장                  | 완료 |
| 3    | 텍스트 청크 분할 (문장 단위, 페이지 경계 유지, 겹침) | 완료 |
| 4    | 임베딩 생성                                          | 예정 |
| 5    | 벡터 저장 및 유사도 검색                             | 예정 |
| 6    | LLM 연동 (검색 결과를 근거로 답변 생성)              | 예정 |
| 7    | 출처 표시 및 오류 처리                               | 예정 |

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
       → 청크 분할 → document_chunks 저장 + CHUNKED
```

- 사용자가 보낸 파일명은 저장 경로에 쓰지 않고, UUID 기반 이름으로 저장합니다.
- DB 등록에 실패하면 저장한 파일을 지워 고아 파일이 남지 않게 합니다.
- 페이지 저장과 상태 변경은 한 트랜잭션으로 묶습니다.
- 추출 텍스트를 페이지 단위로 보관하므로, 청크 분할 방식을 바꿔도 PDF를 다시 읽지 않아도 됩니다.

### 청크 분할 규칙

| 규칙                                                           | 이유                                       |
| -------------------------------------------------------------- | ------------------------------------------ |
| 문장(또는 줄) 단위로 모아 최대 800자에서 끊음                  | 문장 중간에서 잘려 의미가 깨지지 않게      |
| 앞 청크의 마지막 문장들을 최대 150자까지 다음 청크에 다시 포함 | 경계에 걸친 내용도 검색되게                |
| 청크는 페이지를 넘지 않음                                      | 답변 근거를 "3쪽"처럼 정확히 표시하기 위해 |
| 800자보다 긴 문장은 공백 위치에서 강제로 자름                  | 표·목록처럼 마침표 없는 긴 텍스트 대비     |

크기와 겹침은 `application.properties`의 `app.chunk.size`, `app.chunk.overlap`으로 바꿀 수 있습니다.

## 실행 방법

### 1. Oracle 준비

Docker로 Oracle Free를 띄우면 FREEPDB1에 `rag` 계정까지 자동으로 만들어집니다.

```powershell
docker run -d --name oracle-free -p 1521:1521 `
  -e ORACLE_PASSWORD=<비밀번호> -e APP_USER=rag -e APP_USER_PASSWORD=<비밀번호> `
  gvenzl/oracle-free:23-slim
docker logs -f oracle-free   # "DATABASE IS READY TO USE!" 가 나오면 준비 완료
```

앱 계정(`rag`)으로 접속해 [schema-oracle.sql](src/main/resources/db/schema-oracle.sql)을 실행합니다.

```powershell
docker cp src/main/resources/db/schema-oracle.sql oracle-free:/tmp/schema.sql
docker exec -it oracle-free sqlplus rag/<비밀번호>@//localhost/FREEPDB1 "@/tmp/schema.sql"
```

2단계 스키마로 이미 테이블을 만들었다면 [03-add-chunks.sql](src/main/resources/db/migration/03-add-chunks.sql)만 추가로 실행합니다.

### 2. 접속 정보 설정

비밀번호는 코드에 넣지 않고 환경변수로 넘깁니다.

| 환경변수      | 기본값                                        |
| ------------- | --------------------------------------------- |
| `DB_URL`      | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `DB_USERNAME` | `rag`                                         |
| `DB_PASSWORD` | (필수)                                        |

```powershell
$env:DB_PASSWORD = "비밀번호"
.\gradlew.bat bootRun
```

브라우저에서 http://localhost:8080/ 에 접속하면 서버 상태를 확인할 수 있습니다. 업로드는 아래 API를 curl 등으로 호출합니다.

### 3. 빌드·테스트

```powershell
.\gradlew.bat clean build
```

`LlmRagPortfolioApplicationTests`는 스프링 컨텍스트 전체를 띄우므로 Oracle이 실행 중이고 `DB_PASSWORD`가 설정돼 있어야 통과합니다.

## API

| 메서드 | 경로                                     | 설명                                                                        |
| ------ | ---------------------------------------- | --------------------------------------------------------------------------- |
| GET    | `/api/status`                            | 서버 상태 확인                                                              |
| POST   | `/api/documents/upload`                  | 파일 업로드 + 텍스트 추출 (`multipart/form-data`, 필드명 `file`, 최대 20MB) |
| GET    | `/api/documents`                         | 문서 목록 (최근 업로드 순)                                                  |
| GET    | `/api/documents/{id}`                    | 문서 한 건                                                                  |
| GET    | `/api/documents/{id}/pages`              | 페이지 목록과 앞부분 미리보기                                               |
| GET    | `/api/documents/{id}/pages/{pageNumber}` | 한 페이지의 추출 텍스트 전체                                                |
| GET    | `/api/documents/{id}/chunks`             | 청크 목록 (출처 페이지, 본문 포함)                                          |
| POST   | `/api/documents/{id}/chunks`             | 저장된 페이지 텍스트로 청크 다시 만들기 (추출 실패 문서는 409)              |

```bash
curl -F "file=@sample.pdf" http://localhost:8080/api/documents/upload
```

## 프로젝트 구조

```
src/main/java/com/portfolio/llmrag
├── controller   REST API, 공통 예외 처리
├── service      업로드·저장 흐름 (DocumentService, DocumentStorageService)
│   ├── extract  PDF·TXT 텍스트 추출, 텍스트 정리
│   └── chunk    검색용 청크 분할
├── repository   Oracle 접근 (documents, document_pages)
├── domain       테이블 행 모델, 문서 상태
├── dto          API 응답 형식
└── exception    도메인 예외
src/main/resources
├── db/schema-oracle.sql
├── db/migration/     기존 DB 변경 스크립트
└── static/index.html
```
