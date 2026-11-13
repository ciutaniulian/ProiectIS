package ro.expensesplitter.dto;

public record GroupResponse(   Long id,
                               String name,
                               String description,
                               Long createdById) {

}
