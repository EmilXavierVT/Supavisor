package app.entities;

import java.util.EnumSet;
import java.util.Set;

public enum AssignmentState {
    PLANNED,
    ACKNOWLEDGED,
    AUTO_ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    MISSED,
    DECLINED;

    public boolean canTransitionTo(AssignmentState next) {
        return permittedNextStates().contains(next);
    }

    private Set<AssignmentState> permittedNextStates() {
        return switch (this) {
            case PLANNED -> EnumSet.of(ACKNOWLEDGED, AUTO_ACCEPTED, IN_PROGRESS, CANCELLED, MISSED, DECLINED);
            case ACKNOWLEDGED, AUTO_ACCEPTED -> EnumSet.of(IN_PROGRESS, CANCELLED, MISSED, DECLINED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, CANCELLED, MISSED);
            case COMPLETED, CANCELLED, MISSED, DECLINED -> EnumSet.noneOf(AssignmentState.class);
        };
    }
}
