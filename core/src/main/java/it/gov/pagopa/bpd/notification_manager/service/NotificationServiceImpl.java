package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import it.gov.pagopa.bpd.notification_manager.recursion.ConcurrentJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
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

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final WinnersService winnersService;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;
    private final NotificationIOService notificationIOService;
    private final int notifyLoopNumber;
    private final int LIMIT_UPDATE_RANKING_MILESTONE;
    private final Integer MAX_CITIZEN_UPDATE_RANKING_MILESTONE;
    private final int THREAD_POOL;
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
            @Value("${core.NotificationService.notifyWinners.loopNumber}") int notifyLoopNumber,
            NotificationIOService notificationIOService,
            @Value("${core.NotificationService.updateRanking.limitUpdateRankingMilestone}") int LIMIT_UPDATE_RANKING_MILESTONE,
            @Value("${core.NotificationService.updateRanking.maxCitizenUpdateRankingMilestone}") Integer MAX_CITIZEN_UPDATE_RANKING_MILESTONE,
            @Value("${core.NotificationService.updateRanking.threadPoolRankingMilestone}") int THREAD_POOL,
            @Value("${core.NotificationService.update.bonfifica.recesso.citizen.search.days}") int BONIFICA_RECESSO_SEARCH_DAYS) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        this.winnersService = winnersService;
        this.timeToLive = timeToLive;
        this.subject = subject;
        this.markdown = markdown;
        this.notificationIOService = notificationIOService;
        this.notifyLoopNumber = notifyLoopNumber;
        this.LIMIT_UPDATE_RANKING_MILESTONE = LIMIT_UPDATE_RANKING_MILESTONE;
        this.MAX_CITIZEN_UPDATE_RANKING_MILESTONE = MAX_CITIZEN_UPDATE_RANKING_MILESTONE;
        this.THREAD_POOL = THREAD_POOL;
        this.BONIFICA_RECESSO_SEARCH_DAYS = BONIFICA_RECESSO_SEARCH_DAYS;
        this.BONIFICA_RECESSO_SEARCH_DAYS = BONIFICA_RECESSO_SEARCH_DAYS;
        this.CONSAP_TWICE_START_DATE = CONSAP_TWICE_START_DATE;
        this.CONSAP_TWICE_DAYS_FREQUENCY = CONSAP_TWICE_DAYS_FREQUENCY;
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

    @Deprecated
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

    @Deprecated
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
        if (log.isInfoEnabled()) {
            log.info("NotificationServiceImpl.updateBonificaRecesso - start");
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime citizenRange = now.minusDays(BONIFICA_RECESSO_SEARCH_DAYS);

        citizenDAO.updateBonificaRecessoMonolitica(citizenRange.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));

        if (log.isInfoEnabled()) {
            log.info("NotificationServiceImpl.updateBonificaRecesso - end");
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.notify.end.period.scheduled}")
    public void notifyEndPeriodOrEndGracePeriod() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyEndPeriodOrEndGracePeriod start");
        }

        List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

        AwardPeriod awardPeriod = null;
        Boolean isEndPeriod = Boolean.FALSE;
        LocalDate yesterday = LocalDate.now().minus(Period.ofDays(1));

        if (awardPeriods.stream().anyMatch(period ->
                yesterday.isEqual(period.getEndDate())
                        || (yesterday.isAfter(period.getEndDate())
                        && yesterday.isBefore(period.getEndDate().plus(Period.ofDays(period.getGracePeriod().intValue())))))
                || awardPeriods.stream().anyMatch(period ->
                yesterday.isEqual(period.getEndDate().plus(Period.ofDays(period.getGracePeriod().intValue()))))
        ) {

            if (awardPeriods.stream().anyMatch(period ->
                    yesterday.isEqual(period.getEndDate())
                            || (yesterday.isAfter(period.getEndDate())
                            && yesterday.isBefore(period.getEndDate().plus(Period.ofDays(period.getGracePeriod().intValue())))))
            ) {
                isEndPeriod = Boolean.TRUE;
                awardPeriod = awardPeriods.stream().filter(period ->
                        yesterday.isEqual(period.getEndDate())
                                || (yesterday.isAfter(period.getEndDate())
                                && yesterday.isBefore(period.getEndDate().plus(Period.ofDays(period.getGracePeriod().intValue())))
                        )).findAny().get();
            } else {
                awardPeriod = awardPeriods.stream().filter(period ->
                        yesterday.isEqual(period.getEndDate().plus(Period.ofDays(period.getGracePeriod().intValue())))
                ).findAny().get();
            }

            if (awardPeriod != null) {
                notificationIOService.notifyEndPeriodOrEndGracePeriod(awardPeriod, isEndPeriod);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyEndPeriodOrEndGracePeriod end");
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.sendWinnersTwiceWeeks.scheduler}")
    public void sendWinnersTwiceWeeks() throws IOException {

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks start");
        }

        Boolean isTwiceWeeks = Boolean.FALSE;
        LocalDate now = LocalDate.now();

        LocalDate startDate = null;
        try {
            startDate = LocalDate.parse(CONSAP_TWICE_START_DATE, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("NotificationManagerServiceImpl.sendWinnersTwiceWeeks - Unable to format startDate from: " + CONSAP_TWICE_START_DATE);
            }
        }

        if (startDate != null
                && (now.isEqual(startDate)
                || (now.isAfter(startDate)
                && (now.toEpochDay() - startDate.toEpochDay()) % CONSAP_TWICE_DAYS_FREQUENCY == 0))
        ) {
            isTwiceWeeks = Boolean.TRUE;
        }

        if (isTwiceWeeks) {
            List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

            List<Long> endingPeriodId = new ArrayList<>();
            for (AwardPeriod awardPeriod : awardPeriods) {
                if (now.isAfter(awardPeriod.getEndDate()
                        .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                    endingPeriodId.add(awardPeriod.getAwardPeriodId());
                }
            }
            if (!endingPeriodId.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks: ending award period found");
                }
                for (Long aw : endingPeriodId) {
                    sendWinners(aw);
                }

            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks end");
        }
    }
}

