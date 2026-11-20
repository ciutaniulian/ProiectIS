package ro.expensesplitter.service;

import ro.expensesplitter.entity.GroupStatus;

public record GroupStatusChanged(Long groupId, GroupStatus status) {}
