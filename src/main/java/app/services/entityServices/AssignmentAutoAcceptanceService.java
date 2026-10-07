package app.services.entityServices;

import app.dao.AssignmentAutoAcceptanceDAO;
import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AssignmentAutoAcceptanceService {

    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Europe/Copenhagen");

    private final AssignmentAutoAcceptanceDAO autoAcceptanceDAO;
    private final Clock clock;

    public AssignmentAutoAcceptanceService(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, Clock.system(BUSINESS_TIME_ZONE));
    }

    AssignmentAutoAcceptanceService(EntityManagerFactory entityManagerFactory, Clock clock) {
        autoAcceptanceDAO = new AssignmentAutoAcceptanceDAO(entityManagerFactory);
        this.clock = clock == null ? Clock.system(BUSINESS_TIME_ZONE) : clock;
    }

    public int acceptDueAssignments() {
        Instant now = Instant.now(clock);
        LocalDateTime currentDateTime = LocalDateTime.now(clock);
        return autoAcceptanceDAO.acceptEligibleAssignments(
                currentDateTime,
                now,
                assignment -> AssignmentAutoAcceptancePolicy.isEligible(assignment, currentDateTime)
        );
    }
}
