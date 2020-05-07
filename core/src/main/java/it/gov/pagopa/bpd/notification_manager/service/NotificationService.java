package it.gov.pagopa.bpd.notification_manager.service;

import java.util.List;

/**
 * A service to manage the Business Logic related to Notification Manager
 */
public interface NotificationService {

    List<String> findFiscalCodesWithUnsetPayoffInstr();

}
