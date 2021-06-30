package it.gov.pagopa.bpd.notification_manager.service;


import java.io.IOException;
import java.util.List;

/**
 * A service to manage the Business Logic related to Notification Manager
 */
public interface NotificationService {

    void notifyUnsetPayoffInstr();

    @Deprecated
    void updateRanking() throws IOException;

    @Deprecated
    void updateRankingMilestone() throws IOException;

    void testConnection() throws IOException;

    void notifyWinnersPayments() throws IOException;

    void updateBonificaRecesso() throws IOException;

    void notifyEndPeriodOrEndGracePeriod() throws IOException;

    void sendWinnersTwiceWeeks() throws IOException;
}
