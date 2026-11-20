package com.back;

import com.back.domain.AppContext;
import com.back.standard.simpleDb.SimpleDb;
import com.back.standard.util.TestUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class AppTestRunner {
    static {
        AppConfig.setTestMode();
    }

    // 매 테스트 전에 호출: 테스트 DB(wiseSaying__test)의 wise_saying 테이블과 빌드 폴더(db_test/)를 비운다.
    public static void clearDb() {
        AppConfig.setTestMode();

        SimpleDb simpleDb = AppContext.getSimpleDb();
        // 테이블 정의는 schema.sql 과 같다.
        simpleDb.run("""
                CREATE TABLE IF NOT EXISTS wise_saying (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (id),
                    createDate DATETIME NOT NULL,
                    modifyDate DATETIME NOT NULL,
                    content TEXT NOT NULL,
                    author VARCHAR(100) NOT NULL
                )
                """);
        // TRUNCATE 와 같은 효과(행 삭제 + 번호 1부터).
        // 데이터 폴더를 macOS 에 bind mount 한 MySQL 컨테이너에서 TRUNCATE 가 InnoDB assertion 으로 죽는 경우가 있어
        // 파일을 다시 만들지 않는 DELETE + AUTO_INCREMENT 초기화를 쓴다.
        simpleDb.run("DELETE FROM wise_saying");
        simpleDb.run("ALTER TABLE wise_saying AUTO_INCREMENT = 1");

        clearBuildDir();
    }

    private static void clearBuildDir() {
        Path dir = Path.of(AppConfig.getDbDirPath());
        if (!Files.exists(dir)) return;

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String run(String input, Consumer<Scanner> runApp) {
        var bytes = TestUtil.setOutToByteArray();
        try (Scanner scanner = TestUtil.genScanner(input)) {
            runApp.accept(scanner);
            System.out.flush();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            TestUtil.clearSetOutToByteArray();
        }
    }

    // CRUD 테스트용: 샘플 없이 빈 저장소에서 시작
    public static String run(String input) {
        return run(input, scanner -> new App(scanner, false).run());
    }

    // 검색·페이징 테스트용: 샘플 명언 10개로 시작
    public static String runWithSampleData(String input) {
        return run(input, scanner -> new App(scanner, true).run());
    }
}
