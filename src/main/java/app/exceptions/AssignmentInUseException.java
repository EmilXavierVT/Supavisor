package app.exceptions;

/** Thrown when an assignment cannot be deleted because other records still reference it. */
public class AssignmentInUseException extends RuntimeException {
    public AssignmentInUseException(Throwable cause) {
        super("Assignment is in use", cause);
    }
}
