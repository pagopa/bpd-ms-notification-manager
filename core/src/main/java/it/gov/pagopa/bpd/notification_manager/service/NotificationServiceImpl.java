package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.dao.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
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
class NotificationServiceImpl extends BaseService implements NotificationService {

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestClient notificationRestClient;

    @Autowired
    NotificationServiceImpl(CitizenDAO citizenDAO, NotificationDtoMapper notificationDtoMapper,
                            NotificationRestClient notificationRestClient) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestClient = notificationRestClient;
    }


    @Override
    @Scheduled(cron = "0 5 0 * * ?") // Everyday at 00:05 AM
    public void findFiscalCodesWithUnsetPayoffInstr() {
        List<String> citizensFC = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
        for (String citizenCf : citizensFC) {
            NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(citizenCf);
            notificationRestClient.notify(dto);
        }
    }

    @Override
    @Scheduled(cron = "0 47 12 * * ?") // Everyday at 12:47 AM
    public void updateRanking() {
        citizenDAO.update_ranking();
    }
}


