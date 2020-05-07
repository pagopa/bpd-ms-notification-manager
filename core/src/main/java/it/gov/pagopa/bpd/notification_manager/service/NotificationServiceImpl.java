package it.gov.pagopa.bpd.notification_manager.service;


import it.gov.pagopa.bpd.notification_manager.dao.CitizenDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @see NotificationService
 */
@Service
@Slf4j
class NotificationServiceImpl implements NotificationService {

    private final CitizenDAO citizenDAO;

    @Autowired
    NotificationServiceImpl(CitizenDAO citizenDAO) {
        this.citizenDAO = citizenDAO;
    }


    @Override
    @Scheduled(cron = "0 5 0 * * ?") // Everyday at 00:05 AM
    public List<String> findFiscalCodesWithUnsetPayoffInstr() {

        List<String> citizensCF = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
        citizenDAO.update_ranking();
        return citizensCF;
    }
}
