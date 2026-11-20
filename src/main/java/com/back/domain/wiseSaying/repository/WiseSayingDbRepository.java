package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.standard.dto.Page;
import com.back.standard.simpleDb.SimpleDb;
import com.back.standard.simpleDb.Sql;
import com.back.standard.util.JsonUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// MySQL(wise_saying 테이블)에 저장하는 구현체. SQL 은 이 클래스에만 둔다.
public class WiseSayingDbRepository implements WiseSayingRepository {
    private final SimpleDb simpleDb;
    private final Path buildPath;

    public WiseSayingDbRepository(SimpleDb simpleDb, String buildDirPath) {
        this.simpleDb = simpleDb;
        this.buildPath = Path.of(buildDirPath, "data.json");
    }

    @Override
    public void save(WiseSaying wiseSaying) {
        if (wiseSaying.getId() == 0) {
            long newId = simpleDb.genSql()
                    .append("INSERT INTO wise_saying")
                    .append("SET createDate = NOW()")
                    .append(", modifyDate = NOW()")
                    .append(", content = ?", wiseSaying.getContent())
                    .append(", author = ?", wiseSaying.getAuthor())
                    .insert();

            wiseSaying.setId((int) newId);
            return;
        }

        simpleDb.genSql()
                .append("UPDATE wise_saying")
                .append("SET modifyDate = NOW()")
                .append(", content = ?", wiseSaying.getContent())
                .append(", author = ?", wiseSaying.getAuthor())
                .append("WHERE id = ?", wiseSaying.getId())
                .update();
    }

    @Override
    public WiseSaying findById(int id) {
        Map<String, Object> row = simpleDb.genSql()
                .append("SELECT * FROM wise_saying")
                .append("WHERE id = ?", id)
                .selectRow();

        return row == null ? null : toEntity(row);
    }

    @Override
    public List<WiseSaying> findAll() {
        return simpleDb.genSql()
                .append("SELECT * FROM wise_saying")
                .append("ORDER BY id ASC")
                .selectRows()
                .stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public int count() {
        return simpleDb.genSql()
                .append("SELECT COUNT(*) FROM wise_saying")
                .selectLong()
                .intValue();
    }

    // 검색은 WHERE ... LIKE, 정렬은 ORDER BY id DESC, 페이징은 LIMIT/OFFSET 으로 DB 에서 처리한다.
    @Override
    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        Sql countSql = simpleDb.genSql().append("SELECT COUNT(*) FROM wise_saying");
        appendWhere(countSql, keywordType, keyword);
        int totalCount = countSql.selectLong().intValue();

        Sql listSql = simpleDb.genSql().append("SELECT * FROM wise_saying");
        appendWhere(listSql, keywordType, keyword);
        listSql.append("ORDER BY id DESC")
                .append("LIMIT ? OFFSET ?", pageSize, (page - 1) * pageSize);

        List<WiseSaying> content = listSql.selectRows()
                .stream()
                .map(this::toEntity)
                .toList();

        return new Page<>(content, totalCount, page, pageSize);
    }

    private void appendWhere(Sql sql, String keywordType, String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        switch (keywordType) {
            case "author" -> sql.append("WHERE author LIKE CONCAT('%', ?, '%')", keyword);
            case "content" -> sql.append("WHERE content LIKE CONCAT('%', ?, '%')", keyword);
            default -> sql.append("WHERE author LIKE CONCAT('%', ?, '%') OR content LIKE CONCAT('%', ?, '%')", keyword, keyword);
        }
    }

    @Override
    public boolean delete(int id) {
        return simpleDb.genSql()
                .append("DELETE FROM wise_saying")
                .append("WHERE id = ?", id)
                .delete() > 0;
    }

    @Override
    public void build() {
        List<Map<String, Object>> maps = findAll().stream()
                .map(this::toMap)
                .toList();

        try {
            Files.createDirectories(buildPath.getParent());
            Files.writeString(buildPath, JsonUtil.toJsonArray(maps), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getBuildPath() {
        return buildPath;
    }

    private WiseSaying toEntity(Map<String, Object> row) {
        WiseSaying wiseSaying = new WiseSaying((String) row.get("author"), (String) row.get("content"));
        wiseSaying.setId(((Number) row.get("id")).intValue());
        return wiseSaying;
    }

    private Map<String, Object> toMap(WiseSaying wiseSaying) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", wiseSaying.getId());
        map.put("content", wiseSaying.getContent());
        map.put("author", wiseSaying.getAuthor());
        return map;
    }
}
