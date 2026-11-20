package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupStatus;

public record GroupResponse(Long id, String name, String description, long memberCount, GroupStatus status) {}
