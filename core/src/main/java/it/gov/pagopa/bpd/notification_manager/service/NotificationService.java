package it.gov.pagopa.bpd.notification_manager.service;


import java.io.IOException;

/**
 * A service to manage the Business Logic related to Notification Manager
 */
public interface NotificationService {

    void notifyUnsetPayoffInstr();

    void updateRanking() throws IOException;

    void updateRankingMilestone() throws IOException;

    void updateWinners(Long awardPeriodId) throws IOException;

    void sendWinners(Long awardPeriodId) throws IOException;

    void updateAndSendWinners() throws IOException;

    void testConnection() throws IOException;

    void notifyWinnersPayments() throws IOException;

    void updateBonificaRecesso() throws IOException;
}
