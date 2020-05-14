package it.gov.pagopa.bpd.notification_manager.controller;

import eu.sia.meda.core.controller.StatelessController;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
class BpdNotificationManagerControllerImpl extends StatelessController implements BpdNotificationManagerController {

    private final NotificationService notificationService;
    private final NotificationRestClient notificationRestClient;

    @Autowired
    BpdNotificationManagerControllerImpl(NotificationService notificationService, NotificationRestClient notificationRestClient) {
        this.notificationService = notificationService;
        this.notificationRestClient = notificationRestClient;
    }

    @Override
    public void update() {

        notificationService.updateRankingAndFindWinners();
//        notificationRestClient.notify();

    }
}
