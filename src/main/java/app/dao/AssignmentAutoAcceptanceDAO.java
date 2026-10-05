package app.dao;

import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.AssignmentStateHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

public class AssignmentAutoAcceptanceDAO {

    private final EntityManagerFactory entityManagerFactory;

    public AssignmentAutoAcceptanceDAO(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        this.entityManagerFactory = entityManagerFactory;
    }

    public int acceptEligibleAssignments(LocalDateTime currentDateTime, Instant acceptedAt,
                                         Predicate<Assignment> eligibility) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = entityManager.getTransaction();
            try {
                transaction.begin();
                List<Assignment> assignments = entityManager.createQuery("""
                                SELECT assignment FROM Assignment assignment
                                WHERE assignment.isActive = true
                                  AND assignment.assignedEmployee IS NOT NULL
                                  AND assignment.startTime IS NOT NULL
                                  AND assignment.startTime <= :currentDateTime
                                ORDER BY assignment.startTime, assignment.id
                                """, Assignment.class)
                        .setParameter("currentDateTime", currentDateTime)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList();

                List<Assignment> eligibleAssignments = assignments.stream()
                        .filter(eligibility)
                        .toList();
                for (Assignment assignment : eligibleAssignments) {
                    assignment.setState(AssignmentState.AUTO_ACCEPTED);
                    entityManager.persist(new AssignmentStateHistory(
                            assignment.getId(),
                            AssignmentState.PLANNED,
                            AssignmentState.AUTO_ACCEPTED,
                            "system:auto-accept",
                            acceptedAt
                    ));
                }
                transaction.commit();
                return eligibleAssignments.size();
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
}
