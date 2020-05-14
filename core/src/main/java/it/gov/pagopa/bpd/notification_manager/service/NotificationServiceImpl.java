package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.dao.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.dao.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    @Scheduled(cron = "0 40 10 * * ?") // Everyday at 10:40 AM
    public void notifyUnsetPayoffInstr() {
        List<String> citizensFC = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();

        //TODO token mocked
        String TOKEN = "token";
        for (String citizenCf : citizensFC) {
            NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(citizenCf);
            notificationRestClient.notify(dto, TOKEN);
        }
    }

    @Override
    @Scheduled(cron = "0 47 12 * * ?") // Everyday at 12:47 AM
    public void updateRankingAndFindWinners() throws IOException {
        List<WinningCitizen> winners = citizenDAO.updateRankingAndFindWinners();
        if (!winners.isEmpty()) {
            File csvOutputFile = new File("C:/Users/acastagn/Desktop/test.csv");
            List<String> dataLines = new ArrayList<>();
            for (WinningCitizen winner : winners) {
                String sb = winner.getRanking_out().toString() + "," +
                        winner.getFiscal_code_out() + "," +
                        winner.getPayoff_instr_out() + "," +
                        winner.getAw_period_out().toString() + "," +
                        winner.getAw_period_start_out().toString() + "," +
                        winner.getAw_period_end_out().toString();
                dataLines.add(sb);
            }
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                dataLines.forEach(pw::println);
            }
        }
    }
}

