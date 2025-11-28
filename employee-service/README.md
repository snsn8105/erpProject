# Employee Service

ERP 시스템의 직원 관리 서비스입니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.8
- Spring Data JPA
- MySQL 8.0
- Lombok
- Gradle 8.x

## 프로젝트 구조

```
employee-service/
├── src/main/java/com/example/employee/
│   ├── EmployeeServiceApplication.java
│   ├── config/
│   │   └── JpaConfig.java
│   ├── controller/
│   │   └── EmployeeController.java
│   ├── service/
│   │   └── EmployeeService.java
│   ├── repository/
│   │   └── EmployeeRepository.java
│   ├── entity/
│   │   └── Employee.java
│   ├── dto/
│   │   ├── EmployeeCreateRequest.java
│   │   ├── EmployeeUpdateRequest.java
│   │   ├── EmployeeResponse.java
│   │   └── EmployeeCreateResponse.java
│   └── exception/
│       ├── EmployeeNotFoundException.java
│       ├── InvalidUpdateFieldException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── scripts/
│       └── init_mysql.sql
├── build.gradle
└── settings.gradle
```

## 실행 방법

### 1. MySQL 설치 및 데이터베이스 생성

```bash
# MySQL 실행 확인
mysql -u root -p

# SQL 파일 실행
mysql -u root -p < src/main/resources/scripts/init_mysql.sql
```

### 2. 빌드 및 실행

```bash
# Gradle 빌드
./gradlew clean build

# 실행
java -jar build/libs/employee-service-1.0.0.jar

# 또는 Gradle을 통한 실행
./gradlew bootRun
```

서버는 `http://localhost:8081`에서 실행됩니다.

## API 명세

### 1. 직원 생성
```http
POST /employees
Content-Type: application/json

{
  "name": "Kim",
  "department": "HR",
  "position": "Manager"
}

Response (201 Created):
{
  "id": 10
}
```

### 2. 직원 목록 조회
```http
GET /employees

Response (200 OK):
[
  {
    "id": 1,
    "name": "Kim",
    "department": "HR",
    "position": "Manager",
    "createdAt": "2025-01-01T10:00:00"
  }
]
```

### 3. 직원 목록 조회 (필터링)
```http
GET /employees?department=HR&position=Manager

Response (200 OK):
[
  {
    "id": 1,
    "name": "Kim",
    "department": "HR",
    "position": "Manager",
    "createdAt": "2025-01-01T10:00:00"
  }
]
```

### 4. 직원 상세 조회
```http
GET /employees/1

Response (200 OK):
{
  "id": 1,
  "name": "Kim",
  "department": "HR",
  "position": "Manager",
  "createdAt": "2025-01-01T10:00:00"
}
```

### 5. 직원 수정 (department와 position만 가능)
```http
PUT /employees/1
Content-Type: application/json

{
  "department": "Finance",
  "position": "Director"
}

Response (200 OK):
{
  "id": 1,
  "name": "Kim",
  "department": "Finance",
  "position": "Director",
  "createdAt": "2025-01-01T10:00:00"
}
```

### 6. 직원 삭제
```http
DELETE /employees/1

Response (204 No Content)
```

### 7. 직원 존재 여부 확인 (다른 서비스용)
```http
GET /employees/1/exists

Response (200 OK):
true
```

## 테스트 시나리오

### 1. 기본 CRUD 테스트

```bash
# 1. 직원 생성
curl -X POST http://localhost:8081/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Kim","department":"HR","position":"Manager"}'

# 2. 전체 목록 조회
curl http://localhost:8081/employees

# 3. 특정 직원 조회
curl http://localhost:8081/employees/1

# 4. 직원 수정
curl -X PUT http://localhost:8081/employees/1 \
  -H "Content-Type: application/json" \
  -d '{"department":"Finance","position":"Director"}'

# 5. 직원 삭제
curl -X DELETE http://localhost:8081/employees/1
```

### 2. 필터링 테스트

```bash
# 부서별 조회
curl "http://localhost:8081/employees?department=HR"

# 직책별 조회
curl "http://localhost:8081/employees?position=Manager"

# 부서+직책 조회
curl "http://localhost:8081/employees?department=HR&position=Manager"
```

### 3. 유효성 검증 테스트

```bash
# 필수 필드 누락
curl -X POST http://localhost:8081/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Kim"}'

# 존재하지 않는 직원 조회
curl http://localhost:8081/employees/999
```

## 에러 응답 형식

```json
{
  "code": "NOT_FOUND",
  "message": "직원을 찾을 수 없습니다: id=999"
}
```

### 에러 코드
- `NOT_FOUND`: 직원을 찾을 수 없음
- `VALIDATION_ERROR`: 입력 데이터 유효성 검증 실패
- `INVALID_FIELD`: 허용되지 않은 필드 수정 시도
- `INTERNAL_ERROR`: 서버 내부 오류

## 주의사항

1. **Java 버전**: Java 17 이상 필요
2. **수정 제약**: PUT 요청 시 `department`와 `position`만 수정 가능합니다. `name` 필드는 수정할 수 없습니다.
3. **포트 번호**: 서비스는 8081 포트에서 실행됩니다.
4. **데이터베이스**: MySQL 8.0이 실행 중이어야 하며, `erp_db` 데이터베이스가 생성되어 있어야 합니다.

## Gradle 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build

# 의존성 확인
./gradlew dependencies
```

## 로그 확인

애플리케이션 실행 시 다음과 같은 로그를 확인할 수 있습니다:

```
2025-01-01 10:00:00.000  INFO : POST /employees 호출
2025-01-01 10:00:00.100  INFO : 직원 생성 요청: name=Kim, department=HR, position=Manager
2025-01-01 10:00:00.200  INFO : 직원 생성 완료: id=1
```

## 다음 단계

Employee Service가 완료되면 다음 서비스를 개발합니다:
1. Approval Request Service (REST + MongoDB + gRPC Client)
2. Approval Processing Service (REST + gRPC Server + In-Memory)
3. Notification Service (WebSocket)