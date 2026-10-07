package app.exceptions;

/** Thrown when an employee is delegated to an assignment they are already delegated to. */
public class AssignmentAlreadyDelegatedException extends RuntimeException {
    public AssignmentAlreadyDelegatedException(Throwable cause) {
        super("Employee is already delegated to this assignment", cause);
    }
}
