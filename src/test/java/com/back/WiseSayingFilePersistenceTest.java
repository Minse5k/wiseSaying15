package com.back;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// 등록·수정·삭제·빌드 후 앱을 다시 시작(AppTestRunner.run 재호출)해 파일 저장 결과를 확인한다.
class WiseSayingFilePersistenceTest {
    private static final Path DB_DIR = Path.of("db_test/wiseSaying");

    @BeforeEach
    void setUp() {
        AppTestRunner.clearDb();
    }

    private static String read(String fileName) throws IOException {
        return Files.readString(DB_DIR.resolve(fileName), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("테스트는 실제 db/ 가 아닌 db_test/ 를 쓴다")
    void t1() {
        assertThat(AppConfig.getDbDirPath()).isEqualTo("db_test/wiseSaying");
    }

    @Test
    @DisplayName("등록하면 {번호}.json 과 lastId.txt 가 만들어진다")
    void t2() throws IOException {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        assertThat(read("1.json")).isEqualTo("""
                {
                  "id": 1,
                  "content": "현재를 사랑하라.",
                  "author": "작자미상"
                }""");
        assertThat(read("lastId.txt")).isEqualTo("1");
    }

    @Test
    @DisplayName("재시작해도 등록한 명언이 목록에 나온다")
    void t3() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).containsSubsequence(
                "2 / 작자미상 / 과거에 집착하지 마라.",
                "1 / 작자미상 / 현재를 사랑하라."
        );
    }

    @Test
    @DisplayName("재시작 후 등록하면 번호가 이어진다")
    void t4() throws IOException {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        assertThat(out).contains("2번 명언이 등록되었습니다.");
        assertThat(read("lastId.txt")).isEqualTo("2");
    }

    @Test
    @DisplayName("수정하면 파일이 갱신되고 재시작 후에도 유지된다")
    void t5() throws IOException {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                종료
                """);

        assertThat(read("1.json")).contains("\"content\": \"현재와 자신을 사랑하라.\"", "\"author\": \"홍길동\"");

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out)
                .contains("1 / 홍길동 / 현재와 자신을 사랑하라.")
                .doesNotContain("1 / 작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("삭제하면 파일이 지워지고, 삭제한 번호는 재시작 후에도 재사용하지 않는다")
    void t6() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=2
                종료
                """);

        assertThat(DB_DIR.resolve("2.json")).doesNotExist();
        assertThat(DB_DIR.resolve("1.json")).exists();

        String out = AppTestRunner.run("""
                목록
                등록
                미래를 꿈꿔라.
                작자미상
                종료
                """);

        assertThat(out)
                .doesNotContain("2 / 작자미상 / 과거에 집착하지 마라.")
                .contains("1 / 작자미상 / 현재를 사랑하라.", "3번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("빌드하면 모든 명언을 배열 형태의 data.json 으로 만든다")
    void t7() throws IOException {
        String out = AppTestRunner.run("""
                등록
                명언 1
                작가 1
                등록
                명언 2
                작가 2
                빌드
                종료
                """);

        assertThat(out).contains("data.json 파일의 내용이 갱신되었습니다.");
        assertThat(read("data.json")).isEqualTo("""
                [
                  {
                    "id": 1,
                    "content": "명언 1",
                    "author": "작가 1"
                  },
                  {
                    "id": 2,
                    "content": "명언 2",
                    "author": "작가 2"
                  }
                ]""");
    }

    @Test
    @DisplayName("data.json 은 명언 파일로 읽히지 않는다")
    void t8() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                빌드
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 작자미상 / 현재를 사랑하라.", "페이지 : [1]");
    }

    @Test
    @DisplayName("따옴표·역슬래시가 들어간 명언도 저장 후 그대로 읽힌다")
    void t9() {
        AppTestRunner.run("""
                등록
                그는 "괜찮다" 고 말했다 \\ 끝
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 작자미상 / 그는 \"괜찮다\" 고 말했다 \\ 끝");
    }
}
