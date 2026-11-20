package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupActivity;

import java.time.Instant;
import java.util.List;

public record GroupActivityResponse(List<Item> items, boolean hasNext) {
    public record Item(Long id, GroupActivity.Type type, String actorName, String subjectName, Instant createdAt) {}
}
