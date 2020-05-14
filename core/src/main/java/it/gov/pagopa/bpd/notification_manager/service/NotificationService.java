package it.gov.pagopa.bpd.notification_manager.service;


/**
 * A service to manage the Business Logic related to Notification Manager
 */
public interface NotificationService {

    void notifyUnsetPayoffInstr();

    void updateRankingAndFindWinners();

}
