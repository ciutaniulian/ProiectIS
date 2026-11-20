package ro.expensesplitter.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GroupOperationException extends ResponseStatusException {
    private final String code;

    public GroupOperationException(HttpStatus status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
