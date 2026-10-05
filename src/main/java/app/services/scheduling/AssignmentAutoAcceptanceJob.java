package app.services.scheduling;

import app.services.entityServices.AssignmentAutoAcceptanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AssignmentAutoAcceptanceJob implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentAutoAcceptanceJob.class);
    private static final Duration RUN_INTERVAL = Duration.ofSeconds(30);

    private final AssignmentAutoAcceptanceService autoAcceptanceService;
    private final ScheduledExecutorService executorService;

    public AssignmentAutoAcceptanceJob(AssignmentAutoAcceptanceService autoAcceptanceService) {
        if (autoAcceptanceService == null) {
            throw new IllegalArgumentException("AssignmentAutoAcceptanceService cannot be null");
        }
        this.autoAcceptanceService = autoAcceptanceService;
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "assignment-auto-acceptance");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        executorService.scheduleWithFixedDelay(
                this::acceptDueAssignments,
                0,
                RUN_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void acceptDueAssignments() {
        try {
            int acceptedAssignments = autoAcceptanceService.acceptDueAssignments();
            if (acceptedAssignments > 0) {
                LOGGER.info("Automatically accepted {} assignment(s)", acceptedAssignments);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Automatic assignment acceptance failed", exception);
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
