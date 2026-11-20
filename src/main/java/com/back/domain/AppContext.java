package com.back.domain;

import com.back.AppConfig;
import com.back.domain.system.controller.SystemController;
import com.back.domain.wiseSaying.controller.WiseSayingController;
import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.domain.wiseSaying.service.WiseSayingService;
import com.back.standard.simpleDb.SimpleDb;

import java.util.Scanner;

public class AppContext {
    public static Scanner sc;
    public static SimpleDb simpleDb;
    public static WiseSayingRepository wiseSayingRepository;
    public static WiseSayingService wiseSayingService;
    public static WiseSayingController wiseSayingController;
    public static SystemController systemController;

    private static String simpleDbName;

    public static void renew(Scanner scanner) {
        sc = scanner;
        simpleDb = getSimpleDb();
        // 저장소 구현체만 갈아 끼운다: Memory -> File -> Db
        wiseSayingRepository = new WiseSayingDbRepository(simpleDb, AppConfig.getDbDirPath());
        wiseSayingService = new WiseSayingService();
        wiseSayingController = new WiseSayingController();
        systemController = new SystemController();
    }

    // SimpleDb 는 스레드별 커넥션을 재사용하므로 DB 가 바뀔 때만 새로 만든다.
    public static synchronized SimpleDb getSimpleDb() {
        String dbName = AppConfig.getDbName();

        if (simpleDb == null || !dbName.equals(simpleDbName)) {
            if (simpleDb != null) simpleDb.close();

            simpleDb = new SimpleDb(
                    AppConfig.getDbHost(),
                    AppConfig.getDbPort(),
                    AppConfig.getDbUsername(),
                    AppConfig.getDbPassword(),
                    dbName
            );
            simpleDbName = dbName;
        }

        return simpleDb;
    }
}
