package it.gov.pagopa.bpd.notification_manager.recursion;

import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@Slf4j
public class ConcurrentJob implements Callable<Void> {
    private AtomicInteger totalCitizenElab;
    private AtomicBoolean CHECK_CONTINUE_UPDATE_RANKING_MILESTONE;
    private final Integer MAX_CITIZEN_UPDATE_RANKING_MILESTONE;
    private final int LIMIT_UPDATE_RANKING_MILESTONE;
    private final CitizenDAO citizenDAO;
    private final OffsetDateTime timestamp;
    private static int count = 0;

    @Override
    public Void call() {
        if (CHECK_CONTINUE_UPDATE_RANKING_MILESTONE.get() && (MAX_CITIZEN_UPDATE_RANKING_MILESTONE == null || totalCitizenElab.get() < MAX_CITIZEN_UPDATE_RANKING_MILESTONE)) {
            Integer citizenElab = citizenDAO.updateRankingMilestone(0,
                    LIMIT_UPDATE_RANKING_MILESTONE, timestamp);
            totalCitizenElab.addAndGet(citizenElab);
            log.debug("Citizen elaborated: {}", citizenElab);
            log.debug("Total citizen elaborated: {}", totalCitizenElab);
            CHECK_CONTINUE_UPDATE_RANKING_MILESTONE.set(citizenElab == LIMIT_UPDATE_RANKING_MILESTONE);
        }

        return null;
    }
}
