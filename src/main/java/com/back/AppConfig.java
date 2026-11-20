package com.back;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 실행 모드(prod/test)와 DB 접속 정보.
 * 접속 정보는 소스에 두지 않고, 프로젝트 루트의 db.properties(git 제외) 또는 환경변수에서 읽는다.
 * 환경변수가 있으면 db.properties 보다 우선한다.
 */
public final class AppConfig {
    private static final String CONFIG_FILE = "db.properties";

    private static String mode = "prod";
    private static Properties props;

    private AppConfig() {
    }

    public static void setProdMode() {
        mode = "prod";
    }

    public static void setTestMode() {
        mode = "test";
    }

    public static boolean isTestMode() {
        return mode.equals("test");
    }

    // 빌드(data.json) 파일 위치. 테스트는 실제 db/ 가 아닌 db_test/ 를 쓴다.
    public static String getDbDirPath() {
        return isTestMode() ? "db_test/wiseSaying" : "db/wiseSaying";
    }

    public static String getDbHost() {
        return get("DB_HOST", "db.host", "localhost");
    }

    public static int getDbPort() {
        return Integer.parseInt(get("DB_PORT", "db.port", "3306"));
    }

    public static String getDbUsername() {
        return get("DB_USERNAME", "db.username", "root");
    }

    public static String getDbPassword() {
        return get("DB_PASSWORD", "db.password", "");
    }

    // 개발 DB 와 테스트 DB 를 분리한다.
    public static String getDbName() {
        return isTestMode()
                ? get("DB_TEST_NAME", "db.testName", "wiseSaying__test")
                : get("DB_NAME", "db.name", "wiseSaying__dev");
    }

    private static String get(String envKey, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;

        return loadProps().getProperty(propKey, defaultValue);
    }

    private static synchronized Properties loadProps() {
        if (props != null) return props;

        props = new Properties();
        Path path = Path.of(CONFIG_FILE);

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return props;
    }
}
