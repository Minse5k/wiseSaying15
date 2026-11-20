package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.standard.util.JsonUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// 명언 하나를 {dbDir}/{id}.json 파일 하나로 저장한다. 파일 처리는 이 클래스에만 둔다.
public class WiseSayingFileRepository implements WiseSayingRepository {
    private final Path dbDir;

    public WiseSayingFileRepository(String dbDirPath) {
        this.dbDir = Path.of(dbDirPath);
    }

    @Override
    public void save(WiseSaying wiseSaying) {
        if (wiseSaying.getId() == 0) {
            int newId = getLastId() + 1;
            wiseSaying.setId(newId);
            writeFile(getLastIdPath(), String.valueOf(newId));
        }

        writeFile(getFilePath(wiseSaying.getId()), JsonUtil.toJson(toMap(wiseSaying)));
    }

    @Override
    public WiseSaying findById(int id) {
        Path path = getFilePath(id);
        if (!Files.exists(path)) return null;

        return fromJson(readFile(path));
    }

    @Override
    public List<WiseSaying> findAll() {
        if (!Files.isDirectory(dbDir)) return List.of();

        try (Stream<Path> paths = Files.list(dbDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().matches("\\d+\\.json"))
                    .map(path -> fromJson(readFile(path)))
                    .sorted(Comparator.comparingInt(WiseSaying::getId))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean delete(int id) {
        try {
            return Files.deleteIfExists(getFilePath(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void build() {
        List<Map<String, Object>> maps = findAll().stream()
                .map(this::toMap)
                .toList();

        writeFile(getBuildPath(), JsonUtil.toJsonArray(maps));
    }

    public int getLastId() {
        Path path = getLastIdPath();
        if (!Files.exists(path)) return 0;

        try {
            return Integer.parseInt(readFile(path).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Path getFilePath(int id) {
        return dbDir.resolve(id + ".json");
    }

    public Path getLastIdPath() {
        return dbDir.resolve("lastId.txt");
    }

    public Path getBuildPath() {
        return dbDir.resolve("data.json");
    }

    private Map<String, Object> toMap(WiseSaying wiseSaying) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", wiseSaying.getId());
        map.put("content", wiseSaying.getContent());
        map.put("author", wiseSaying.getAuthor());
        return map;
    }

    private WiseSaying fromJson(String json) {
        Map<String, String> map = JsonUtil.parseObject(json);

        WiseSaying wiseSaying = new WiseSaying(map.get("author"), map.get("content"));
        wiseSaying.setId(Integer.parseInt(map.get("id")));
        return wiseSaying;
    }

    private void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
