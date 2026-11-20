package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.standard.dto.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface WiseSayingRepository {
    void save(WiseSaying wiseSaying);

    WiseSaying findById(int id);

    // id 오름차순 전체 목록
    List<WiseSaying> findAll();

    boolean delete(int id);

    void build();

    default void edit(WiseSaying item, String author, String content) {
        item.setAuthor(author);
        item.setContent(content);
        save(item);
    }

    default int count() {
        return findAll().size();
    }

    default List<WiseSaying> reverseList() {
        List<WiseSaying> reverseList = new ArrayList<>(findAll());
        Collections.reverse(reverseList);
        return reverseList;
    }

    default Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        List<WiseSaying> filtered = reverseList().stream()
                .filter(e -> matches(e, keywordType, keyword))
                .toList();

        int from = Math.min((page - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());

        return new Page<>(filtered.subList(from, to), filtered.size(), page, pageSize);
    }

    private static boolean matches(WiseSaying wiseSaying, String keywordType, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;

        return switch (keywordType) {
            case "author" -> wiseSaying.getAuthor().contains(keyword);
            case "content" -> wiseSaying.getContent().contains(keyword);
            default -> wiseSaying.getAuthor().contains(keyword) || wiseSaying.getContent().contains(keyword);
        };
    }
}
