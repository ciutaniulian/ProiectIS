package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupRole;

public record GroupResponse(   Long id,
                               String name,
                               String description,
                               Long createdById,
                               GroupRole role) {

}
