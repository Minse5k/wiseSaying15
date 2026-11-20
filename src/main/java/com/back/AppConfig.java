package com.back;

public final class AppConfig {
    private static String mode = "prod";

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

    // 테스트는 실제 db/ 가 아닌 db_test/ 를 쓴다.
    public static String getDbDirPath() {
        return isTestMode() ? "db_test/wiseSaying" : "db/wiseSaying";
    }
}
