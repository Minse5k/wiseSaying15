package com.back.standard.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 외부 라이브러리 없이 평평한(중첩 없는) JSON 객체를 만들고 읽는다.
public final class JsonUtil {
    private static final String INDENT = "  ";

    private JsonUtil() {
    }

    public static String toJson(Map<String, Object> map) {
        return toJson(map, "");
    }

    private static String toJson(Map<String, Object> map, String baseIndent) {
        String body = map.entrySet().stream()
                .map(e -> baseIndent + INDENT + quote(e.getKey()) + ": " + toJsonValue(e.getValue()))
                .collect(Collectors.joining(",\n"));

        return baseIndent + "{\n" + body + "\n" + baseIndent + "}";
    }

    public static String toJsonArray(List<Map<String, Object>> maps) {
        if (maps.isEmpty()) return "[]";

        String body = maps.stream()
                .map(map -> toJson(map, INDENT))
                .collect(Collectors.joining(",\n"));

        return "[\n" + body + "\n]";
    }

    private static String toJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return quote(value.toString());
    }

    private static String quote(String str) {
        StringBuilder sb = new StringBuilder("\"");

        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }

        return sb.append('"').toString();
    }

    // 값은 문자열로 돌려준다. 숫자는 "1" 처럼 원문 그대로, null 은 null.
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        Parser p = new Parser(json);

        p.skipWhitespace();
        p.expect('{');
        p.skipWhitespace();

        if (p.peek() == '}') return map;

        while (true) {
            p.skipWhitespace();
            String key = p.readString();
            p.skipWhitespace();
            p.expect(':');
            p.skipWhitespace();
            map.put(key, p.readValue());
            p.skipWhitespace();

            char c = p.next();
            if (c == '}') return map;
            if (c != ',') throw new IllegalArgumentException("잘못된 JSON: " + json);
        }
    }

    private static class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        char peek() {
            return src.charAt(pos);
        }

        char next() {
            return src.charAt(pos++);
        }

        void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("'" + c + "' 이(가) 필요합니다: " + src);
        }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        String readValue() {
            if (peek() == '"') return readString();

            int start = pos;
            while (pos < src.length() && ",}".indexOf(src.charAt(pos)) < 0 && !Character.isWhitespace(src.charAt(pos))) pos++;
            String raw = src.substring(start, pos);

            return raw.equals("null") ? null : raw;
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();

            while (true) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }

                char escaped = next();
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> sb.append(escaped);
                }
            }
        }
    }
}
