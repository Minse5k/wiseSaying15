package com.back.standard.simpleDb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sql {
    // 컬럼명 = 필드명 으로 매핑한다(isBlind 처럼 is 로 시작하는 boolean 필드도 그대로).
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append('\n');
        sqlBuilder.append(sqlPart);
        Collections.addAll(this.params, params);
        return this;
    }

    // "IN (?)" 의 ? 를 값 개수만큼 "?, ?, ?" 로 펼친다.
    public Sql appendIn(String sqlPart, Object... params) {
        String placeholders = Arrays.stream(params)
                .map(p -> "?")
                .collect(Collectors.joining(", "));

        return append(sqlPart.replace("?", placeholders), params);
    }

    public long insert() {
        return simpleDb.insert(getSql(), params);
    }

    public int update() {
        return simpleDb.update(getSql(), params);
    }

    public int delete() {
        return simpleDb.update(getSql(), params);
    }

    public List<Map<String, Object>> selectRows() {
        return simpleDb.selectRows(getSql(), params);
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return selectRows().stream()
                .map(row -> OBJECT_MAPPER.convertValue(row, cls))
                .toList();
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();
        return row == null ? null : OBJECT_MAPPER.convertValue(row, cls);
    }

    public LocalDateTime selectDatetime() {
        return (LocalDateTime) selectValue();
    }

    public Long selectLong() {
        return toLong(selectValue());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(row -> toLong(row.values().iterator().next()))
                .toList();
    }

    public String selectString() {
        Object value = selectValue();
        return value == null ? null : value.toString();
    }

    // BIT(1) 은 Boolean, "1 = 1" 같은 비교식은 숫자(1/0)로 온다.
    public Boolean selectBoolean() {
        Object value = selectValue();

        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.longValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private Object selectValue() {
        Map<String, Object> row = selectRow();
        return row == null ? null : row.values().iterator().next();
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private String getSql() {
        return sqlBuilder.toString();
    }

    @Override
    public String toString() {
        return getSql();
    }
}
