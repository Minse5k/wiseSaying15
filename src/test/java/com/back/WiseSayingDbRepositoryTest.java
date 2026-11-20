package com.back;

import com.back.domain.AppContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// MySQL 저장소 통합 테스트. 앱을 다시 시작(AppTestRunner.run 재호출)해 DB 에 남은 결과를 확인한다.
class WiseSayingDbRepositoryTest {
    @BeforeEach
    void setUp() {
        AppTestRunner.clearDb();
    }

    private static Map<String, Object> findRow(int id) {
        return AppContext.getSimpleDb().genSql()
                .append("SELECT * FROM wise_saying WHERE id = ?", id)
                .selectRow();
    }

    private static long countRows() {
        return AppContext.getSimpleDb().genSql()
                .append("SELECT COUNT(*) FROM wise_saying")
                .selectLong();
    }

    @Test
    @DisplayName("테스트는 개발 DB 가 아닌 테스트 DB 를 쓴다")
    void t1() {
        assertThat(AppConfig.getDbName()).isEqualTo("wiseSaying__test");
    }

    @Test
    @DisplayName("등록하면 wise_saying 테이블에 행이 생긴다")
    void t2() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        Map<String, Object> row = findRow(1);

        assertThat(row)
                .containsEntry("content", "현재를 사랑하라.")
                .containsEntry("author", "작자미상");
        assertThat(row.get("createDate")).isNotNull();
        assertThat(countRows()).isEqualTo(1);
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
    void t4() {
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
    }

    @Test
    @DisplayName("수정하면 DB 행이 바뀌고 재시작 후에도 유지된다")
    void t5() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                종료
                """);

        assertThat(findRow(1))
                .containsEntry("content", "현재와 자신을 사랑하라.")
                .containsEntry("author", "홍길동");

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out)
                .contains("1 / 홍길동 / 현재와 자신을 사랑하라.")
                .doesNotContain("1 / 작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("삭제하면 행이 지워지고, 삭제한 번호는 재시작 후에도 재사용하지 않는다")
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

        assertThat(findRow(2)).isNull();
        assertThat(findRow(1)).isNotNull();

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
    @DisplayName("빌드하면 DB 의 모든 명언을 배열 형태의 data.json 으로 만든다")
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
        assertThat(Files.readString(Path.of("db_test/wiseSaying/data.json"), StandardCharsets.UTF_8)).isEqualTo("""
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
    @DisplayName("따옴표·역슬래시·% 가 들어간 명언도 그대로 저장되고 검색된다")
    void t8() {
        AppTestRunner.run("""
                등록
                그는 "100%" 라고 말했다 \\ 끝
                작자미상
                등록
                평범한 명언
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록?keywordType=content&keyword=100%
                종료
                """);

        assertThat(out)
                .contains("1 / 작자미상 / 그는 \"100%\" 라고 말했다 \\ 끝")
                .doesNotContain("2 / 작자미상 / 평범한 명언");
    }

    @Test
    @DisplayName("앱 시작 시 DB 에 명언이 0개면 샘플 10개를 저장하고, 있으면 다시 만들지 않는다")
    void t9() {
        AppTestRunner.runWithSampleData("""
                종료
                """);

        assertThat(countRows()).isEqualTo(10);

        AppTestRunner.runWithSampleData("""
                종료
                """);

        assertThat(countRows()).isEqualTo(10);
    }
}
