package it.gov.pagopa.bpd.notification_manager.controller;

import eu.sia.meda.core.controller.StatelessController;
import it.gov.pagopa.bpd.notification_manager.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
class BpdNotificationManagerControllerImpl extends StatelessController implements BpdNotificationManagerController {

    private final NotificationService notificationService;

    @Autowired
    BpdNotificationManagerControllerImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public List<String> find() {
        return notificationService.findFiscalCodesWithUnsetPayoffInstr();
    }
}
