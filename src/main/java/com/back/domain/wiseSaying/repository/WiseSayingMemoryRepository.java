package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;

import java.util.ArrayList;
import java.util.List;

public class WiseSayingMemoryRepository implements WiseSayingRepository {
    private final List<WiseSaying> wiseList = new ArrayList<>();
    private int lastId = 0;

    @Override
    public void save(WiseSaying wiseSaying) {
        if (wiseSaying.getId() != 0) return;

        wiseSaying.setId(++lastId);
        wiseList.add(wiseSaying);
    }

    @Override
    public WiseSaying findById(int id) {
        return wiseList.stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<WiseSaying> findAll() {
        return new ArrayList<>(wiseList);
    }

    @Override
    public boolean delete(int id) {
        return wiseList.removeIf(e -> e.getId() == id);
    }

    @Override
    public void build() {
        throw new UnsupportedOperationException("메모리 저장소는 빌드를 지원하지 않습니다.");
    }
}
