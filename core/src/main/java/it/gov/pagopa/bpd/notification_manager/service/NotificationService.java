package it.gov.pagopa.bpd.notification_manager.service;


import java.io.IOException;

/**
 * A service to manage the Business Logic related to Notification Manager
 */
public interface NotificationService {

    void notifyUnsetPayoffInstr();

    void updateRankingAndWinners() throws IOException;

    void findWinners();



}
