package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import it.gov.pagopa.bpd.notification_manager.recursion.ConcurrentJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int LIMIT_UPDATE_RANKING_MILESTONE;
    private final Integer MAX_CITIZEN_UPDATE_RANKING_MILESTONE;
    private final int THREAD_POOL;
    private final boolean updateStatusEnabled;

    private final int BONIFICA_RECESSO_SEARCH_DAYS;

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
            @Value("${core.NotificationService.updateRanking.limitUpdateRankingMilestone}") int LIMIT_UPDATE_RANKING_MILESTONE,
            @Value("${core.NotificationService.updateRanking.maxCitizenUpdateRankingMilestone}") Integer MAX_CITIZEN_UPDATE_RANKING_MILESTONE,
            @Value("${core.NotificationService.updateRanking.threadPoolRankingMilestone}") int THREAD_POOL,
            @Value("${core.NotificationService.update.bonfifica.recesso.citizen.search.days}") int BONIFICA_RECESSO_SEARCH_DAYS,
            @Value("${core.NotificationService.findWinners.updateStatus.enable}") boolean updateStatusEnabled) {
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
        this.notificationIOService = notificationIOService;
        this.notifyLoopNumber = notifyLoopNumber;
        this.LIMIT_UPDATE_RANKING_MILESTONE = LIMIT_UPDATE_RANKING_MILESTONE;
        this.MAX_CITIZEN_UPDATE_RANKING_MILESTONE = MAX_CITIZEN_UPDATE_RANKING_MILESTONE;
        this.THREAD_POOL = THREAD_POOL;
        this.BONIFICA_RECESSO_SEARCH_DAYS=BONIFICA_RECESSO_SEARCH_DAYS;
        this.updateStatusEnabled = updateStatusEnabled;
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

        log.trace("From updateRanking call updateRankingMilestone");
        updateRankingMilestone();

    }

    public void updateRankingMilestone() {
        if (logger.isInfoEnabled()) {
            logger.info("Executing procedure: updateRankingMilestone");
        }

        ForkJoinPool pool = null;
        AtomicBoolean CHECK_CONTINUE_UPDATE_RANKING_MILESTONE = new AtomicBoolean(true);
        AtomicInteger totalCitizenElab = new AtomicInteger(0);

        try {
            OffsetDateTime timestamp = OffsetDateTime.now();

            pool = (ForkJoinPool) Executors.newWorkStealingPool(THREAD_POOL);
            for (int threadCount = 0; threadCount < THREAD_POOL; threadCount++) {
                ConcurrentJob concurrentJob = new ConcurrentJob(totalCitizenElab, CHECK_CONTINUE_UPDATE_RANKING_MILESTONE, MAX_CITIZEN_UPDATE_RANKING_MILESTONE, LIMIT_UPDATE_RANKING_MILESTONE, citizenDAO, timestamp);
                pool.execute(concurrentJob);
            }

        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }

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
            ////////////////////////////
            if (logger.isDebugEnabled()) {
                logger.debug("WinnersServiceImpl.sendWinners start");
            }
            if (tempDir == null) {
                throw new IllegalArgumentException("tempDir cannot be null");
            }

            int result = -1;

            List<WinningCitizen> winners;
            if (updateStatusEnabled) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Starting findWinners query with limit = %d", maxRow));
                }
                winners = citizenDAO.findWinners(awardPeriodId, maxRow);
            } else {
                long offset = maxRow * fileChunkCount;
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Starting findWinners query with offset %d and limit = %d", offset, maxRow));
                }
                winners = citizenDAO.findWinners(awardPeriodId, offset, maxRow);
            }
            fetchedRecord = winners.size();
            if (logger.isInfoEnabled()) {
                logger.info("Search for winners finished");
            }

            if (updateStatusEnabled && !winners.isEmpty()) {
                winners.forEach(w -> w.setStatus(WinningCitizen.Status.WORKING));
                citizenDAO.saveAll(winners);
            }

////////////////////////////
            winnersService.sendWinners(winners, awardPeriodId, fileChunkCount, tempDir, timestamp, recordTotCount);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("WinnersServiceImpl.sendWinners end with %d processed records", fetchedRecord));
            }


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

        do {
            if (notifyLoopNumber == -1 || loopNumber < notifyLoopNumber) {
                itemCount = notificationIOService.notifyWinnersPayments();
                loopNumber++;
            } else {
                itemCount = 0;
            }
        } while (itemCount > 0);
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.update.bonfifica.recesso.schedule}")
    public void updateBonificaRecesso() throws IOException {
        if(log.isInfoEnabled()){
            log.info("NotificationServiceImpl.updateBonificaRecesso - start");
        }
        OffsetDateTime now=OffsetDateTime.now();
        OffsetDateTime citizenRange = now.minusDays(BONIFICA_RECESSO_SEARCH_DAYS);

        citizenDAO.updateBonificaRecessoMonolitica(citizenRange.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));

        if(log.isInfoEnabled()){
            log.info("NotificationServiceImpl.updateBonificaRecesso - end");
        }
    }
}

