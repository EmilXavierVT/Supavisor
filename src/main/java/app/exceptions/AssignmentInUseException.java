package app.exceptions;

public class AssignmentInUseException extends RuntimeException {
    public AssignmentInUseException(Throwable cause) {
        super("Assignment is in use", cause);
    }
}
