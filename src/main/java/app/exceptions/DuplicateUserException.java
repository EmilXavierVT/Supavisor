package app.exceptions;

public class DuplicateUserException extends ValidationException {
    public DuplicateUserException(String message) {
        super(message);
    }
}
