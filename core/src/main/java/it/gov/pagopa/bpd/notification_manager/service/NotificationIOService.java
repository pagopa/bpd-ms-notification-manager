package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;

import java.io.IOException;

public interface NotificationIOService {

    public int notifyWinnersPayments() throws IOException;

    public NotificationResource sendNotifyIO(String fiscalCode, String notifySubject, String notifyMarkdown);
}
