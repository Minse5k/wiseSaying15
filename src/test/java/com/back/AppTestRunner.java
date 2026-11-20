package com.back;

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

    // 테스트 전용 저장 폴더(db_test/)를 통째로 비운다. 매 테스트 전에 호출.
    public static void clearDb() {
        AppConfig.setTestMode();
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
