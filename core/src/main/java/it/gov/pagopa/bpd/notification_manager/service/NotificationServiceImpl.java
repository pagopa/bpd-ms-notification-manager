package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final NotificationRestConnector notificationRestConnector;
    private final String outputFilePath;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;

    @Autowired
    NotificationServiceImpl(
            CitizenDAO citizenDAO, NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            @Value("${core.NotificationService.updateRankingAndFindWinners.outputFilePath}") String outputFilePath,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.outputFilePath = outputFilePath;
        this.timeToLive = timeToLive;
        this.subject = subject;
        this.markdown = markdown;
    }


    @Override
    @Scheduled(cron = "${core.NotificationService.notifyUnsetPayoffInstr.scheduler}")
    public void notifyUnsetPayoffInstr() {
        try {
            List<String> citizensFC = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
            for (String citizenCf : citizensFC) {
                NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                        citizenCf, timeToLive, subject, markdown);
                NotificationResource resouce = notificationRestConnector.notify(dto);
                if (logger.isDebugEnabled()) {
                    logger.debug("Notified citizen, notification id: " + resouce.getId());
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateRankingAndFindWinners.scheduler}")
    public void updateRankingAndFindWinners() throws IOException {
        try {

            List<WinningCitizen> winners = citizenDAO.updateRankingAndFindWinners();
            if (!winners.isEmpty()) {
                List<WinningCitizen> winnersForCSV = new ArrayList<>();
                for (WinningCitizen winningCitizen : winners)
                    if (winningCitizen.getPayoffInstr() != null && !winningCitizen.getPayoffInstr().isEmpty())
                        winnersForCSV.add(winningCitizen);
                File csvOutputFile = new File(outputFilePath);
                List<String> dataLines = new ArrayList<>();
                for (WinningCitizen winnerForCSV : winnersForCSV) {
                    String sb = winnerForCSV.getAmount().toString() + "," +
                            winnerForCSV.getFiscalCode() + "," +
                            winnerForCSV.getPayoffInstr() + "," +
                            winnerForCSV.getAwardPeriodId().toString() + "," +
                            winnerForCSV.getAwardPeriodStart().toString() + "," +
                            winnerForCSV.getAwardPeriodEnd().toString();
                    dataLines.add(sb);
                }
                try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                    dataLines.forEach(pw::println);
                }
            }
            //TODO: Send results through SFTP channel
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

