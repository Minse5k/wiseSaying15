# 명언 앱 ⑧ MySQL 전환 (15단계)

파일(`db/wiseSaying/{id}.json`)에 저장하던 명언을 MySQL 에 저장하도록 바꾼 콘솔 명언 앱입니다.
등록·목록·삭제·수정·검색·페이징·빌드가 모두 MySQL 저장소로 동작합니다.

## 1. MySQL 띄우기

```bash
docker run --name mysql-1 -p 127.0.0.1:3306:3306 -e TZ=Asia/Seoul -e MYSQL_ROOT_PASSWORD=<비밀번호> -d mysql:8.4
docker exec mysql-1 mysqladmin ping -uroot -p<비밀번호>
```

## 2. DB·테이블 만들기

개발 DB(`wiseSaying__dev`)와 테스트 DB(`wiseSaying__test`)를 분리합니다.
두 DB 와 `wise_saying` 테이블을 만드는 SQL 은 [`schema.sql`](schema.sql) 에 있습니다.

```bash
docker exec -i mysql-1 mysql -uroot -p<비밀번호> < schema.sql
```

```sql
CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id),
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL
);
```

명언 번호는 `AUTO_INCREMENT` 가 매기므로, 재시작해도 번호가 이어지고 삭제한 번호는 다시 쓰지 않습니다.

## 3. 접속 정보 설정

접속 정보는 소스에 하드코딩하지 않습니다. `db.properties.example` 을 `db.properties` 로 복사해 값을 채우세요.
`db.properties` 는 `.gitignore` 에 들어 있어 저장소에 올라가지 않습니다.

```properties
db.host=localhost
db.port=3306
db.username=root
db.password=<비밀번호>
db.name=wiseSaying__dev
db.testName=wiseSaying__test
```

환경변수 `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `DB_TEST_NAME` 이 있으면 `db.properties` 보다 우선합니다.
(읽는 코드: `src/main/java/com/back/AppConfig.java`)

## 4. 실행·테스트

```bash
./gradlew test
```

- 테스트는 `AppConfig.setTestMode()` 로 테스트 DB(`wiseSaying__test`)를 씁니다.
- 매 테스트 전에 `AppTestRunner.clearDb()` 가 `wise_saying` 테이블을 비우고(행 삭제 + `AUTO_INCREMENT = 1`) 빌드 폴더 `db_test/` 를 지웁니다.
  - `TRUNCATE` 와 같은 효과입니다. 데이터 폴더를 macOS 에 bind mount 한 MySQL 컨테이너에서 `TRUNCATE` 가 InnoDB assertion 으로 죽는 경우가 있어 `DELETE` 를 씁니다.
- `WiseSayingDbRepositoryTest` 는 등록·수정·삭제 후 앱을 다시 시작해 DB 에 남은 결과, 번호 유지, `data.json` 빌드를 확인합니다.
- 기존 통합 테스트(`WiseSayingControllerTest`, `WiseSayingListTest`)는 그대로 통과합니다.

## 5. 리포지터리를 어떻게 갈아 끼웠나

```
WiseSayingRepository (interface)
├── WiseSayingMemoryRepository   ← 명언 앱 ①~⑥ (메모리)
├── WiseSayingFileRepository     ← 명언 앱 ⑦ (파일, JSON)
└── WiseSayingDbRepository       ← 명언 앱 ⑧ (MySQL)  ★ 지금 쓰는 구현체
```

- `AppContext.renew()` 에서 저장소를 만드는 한 줄만 `WiseSayingDbRepository` 로 바꿨습니다.
  컨트롤러·서비스 코드는 바꾸지 않았습니다.
- DB 접근은 앞 과제에서 만든 `SimpleDb`/`Sql`(JDBC 래퍼, `com.back.standard.simpleDb`)을 재사용합니다.
  SQL 은 `WiseSayingDbRepository` 에만 있습니다.
- 검색·페이징은 SQL 로 처리합니다.

```sql
SELECT COUNT(*) FROM wise_saying WHERE content LIKE CONCAT('%', ?, '%');
SELECT * FROM wise_saying WHERE content LIKE CONCAT('%', ?, '%') ORDER BY id DESC LIMIT ? OFFSET ?;
```

- `빌드` 명령은 DB 의 모든 명언을 읽어 `db/wiseSaying/data.json` 으로 저장합니다.
