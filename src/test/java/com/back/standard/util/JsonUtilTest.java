package com.back.standard.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilTest {
    @Test
    @DisplayName("Map -> JSON 문자열")
    void t1() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1);
        map.put("content", "명언 1");
        map.put("author", "작가 1");

        assertThat(JsonUtil.toJson(map)).isEqualTo("""
                {
                  "id": 1,
                  "content": "명언 1",
                  "author": "작가 1"
                }""");
    }

    @Test
    @DisplayName("JSON 문자열 -> Map, 이스케이프 왕복")
    void t2() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 7);
        map.put("content", "줄\n바꿈 \"따옴표\" \\역슬래시");

        Map<String, String> parsed = JsonUtil.parseObject(JsonUtil.toJson(map));

        assertThat(parsed)
                .containsEntry("id", "7")
                .containsEntry("content", "줄\n바꿈 \"따옴표\" \\역슬래시");
    }
}
