package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupStatus;

import java.util.UUID;

public record ChatReply(Long groupId, UUID requestId, Result result, GroupStatus status, ChatMessageResponse message) {
    public enum Result { READY, SENT, ARCHIVED, ACCESS_LOST, FAILED }
}
