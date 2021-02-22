package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import feign.FeignException;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.exception.NotifyTooManyRequestException;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.AwardWinnerErrorDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.AwardWinnerError;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @see NotificationService
 */
@Service
@Slf4j
class NotificationServiceImpl extends BaseService implements NotificationService {

    private static final DateTimeFormatter ONLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final WinnersService winnersService;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;
    private final Long maxRow;
    private final boolean deleteTmpFilesEnable;
    private final NotificationIOService notificationIOService;
    private final int notifyLoopNumber;
    private final Long awardPeriodUpdateRankingMilestone;
    private final Integer limitUpdateRankingMilestone;

    @Autowired
    NotificationServiceImpl(
            CitizenDAO citizenDAO,
            NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            AwardPeriodRestClient awardPeriodRestClient,
            WinnersService winnersService,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown,
            @Value("${core.NotificationService.findWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.findWinners.deleteTmpFiles.enable}") boolean deleteTmpFilesEnable,
            @Value("${core.NotificationService.notifyWinners.loopNumber}") int notifyLoopNumber,
            NotificationIOService notificationIOService,
            @Value("${core.NotificationService.updateRanking.awardPeriodUpdateRankingMilestone}") Long awardPeriodUpdateRankingMilestone,
            @Value("${core.NotificationService.updateRanking.limitUpdateRankingMilestone}") Integer limitUpdateRankingMilestone) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        this.winnersService = winnersService;
        this.timeToLive = timeToLive;
        this.subject = subject;
        this.markdown = markdown;
        this.maxRow = maxRow;
        this.deleteTmpFilesEnable = deleteTmpFilesEnable;
        this.notificationIOService=notificationIOService;
        this.notifyLoopNumber=notifyLoopNumber;
        this.awardPeriodUpdateRankingMilestone = awardPeriodUpdateRankingMilestone;
        this.limitUpdateRankingMilestone = limitUpdateRankingMilestone;
    }


    @Override
    @Scheduled(cron = "${core.NotificationService.notifyUnsetPayoffInstr.scheduler}")
    public void notifyUnsetPayoffInstr() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.notifyUnsetPayoffInstr start");
            }
            List<String> citizensFC = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
            if (citizensFC != null && !citizensFC.isEmpty()) {
                for (String citizenCf : citizensFC) {
                    NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                            citizenCf, timeToLive, subject, markdown);
                    NotificationResource resource = notificationRestConnector.notify(dto);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Notified citizen, notification id: " + resource.getId());
                    }
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.notifyUnsetPayoffInstr end");
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateRanking.scheduler}")
    public void updateRanking() {
        if (logger.isInfoEnabled()) {
            logger.info("Executing procedure: updateRanking");
        }
        citizenDAO.updateRanking();
        if (logger.isInfoEnabled()) {
            logger.info("Executed procedure: updateRanking");
        }

        if (logger.isInfoEnabled()) {
            logger.info("Executing procedure: updateRankingMilestone");
        }
        int offset = 0;
        int citizenCount;
        do {
            citizenCount = citizenDAO.updateRankingMilestone(awardPeriodUpdateRankingMilestone, offset, limitUpdateRankingMilestone);
            offset += citizenCount;
        } while(citizenCount >= limitUpdateRankingMilestone);
        if (logger.isInfoEnabled()) {
            logger.info("Executed procedure: updateRankingMilestone");
        }

    }

    @Override
    public void updateWinners(Long awardPeriodId) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Executing procedure updateWinners for awardPeriod %s", awardPeriodId));
        }
        citizenDAO.updateWinners(awardPeriodId);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Executed procedure updateWinners for awardPeriod %s", awardPeriodId));
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateAndSendWinners.scheduler}")
    public void updateAndSendWinners() throws IOException {

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.updateAndSendWinners start");
        }

        List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

        Long endingPeriodId = null;
        for (AwardPeriod awardPeriod : awardPeriods) {
            if (LocalDate.now().equals(awardPeriod.getEndDate()
                    .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                endingPeriodId = awardPeriod.getAwardPeriodId();
            }
        }
        if (endingPeriodId != null) {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.updateAndSendWinners: ending award period found");
            }
            updateWinners(endingPeriodId);
            sendWinners(endingPeriodId);
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.updateAndSendWinners end");
        }


    }

    @Override
    public void sendWinners(Long awardPeriodId) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinners start");
        }

        int fileChunkCount = 0;
        int fetchedRecord;

        LocalDateTime timestamp = LocalDateTime.now();
        int recordTotCount = citizenDAO.countFindWinners(awardPeriodId);

        Path tempDir = Files.createTempDirectory("csv_directory");
        if (logger.isInfoEnabled()) {
            logger.info(String.format("temporaryDirectoryPath = %s", tempDir.toAbsolutePath().toString()));
        }

        do {
            fetchedRecord = winnersService.sendWinners(awardPeriodId, fileChunkCount, tempDir, timestamp, recordTotCount);
            fileChunkCount++;

        } while (fetchedRecord == maxRow);

        if (deleteTmpFilesEnable) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Deleting %s", tempDir));
            }
            deleteDirectoryRecursion(tempDir);
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinners end");
        }
    }


    private void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }


    @Override
    public void testConnection() throws IOException {
        winnersService.testConnection();
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.notifyWinners.scheduler}")
    public void notifyWinnersPayments() throws IOException {
        int itemCount = 0;
        int loopNumber = 0;

        do{
            if(notifyLoopNumber==-1 || loopNumber<notifyLoopNumber){
                itemCount=notificationIOService.notifyWinnersPayments();
                loopNumber++;
            }else{
                itemCount = 0;
            }
        } while(itemCount>0);
    }


}

