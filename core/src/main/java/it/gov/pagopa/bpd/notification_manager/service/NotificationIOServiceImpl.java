package it.gov.pagopa.bpd.notification_manager.service;

import eu.sia.meda.service.BaseService;
import feign.FeignException;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.exception.NotifyTooManyRequestException;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.AwardWinnerErrorDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenRankingDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.AwardWinnerError;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.CitizenRanking;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ROUND_HALF_DOWN;

@Service
@Slf4j
public class NotificationIOServiceImpl extends BaseService implements NotificationIOService {

    private static final DateTimeFormatter ONLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final Long timeToLive;
    private final Long maxNotifyRow;
    private final Long maxNotifyTimes;
    private final String notifyMarkdownOK;
    private final String notifyMarkdownTechnicalOK;
    private final String notifyMarkdownKO;
    private final String notifyMarkdownTechnicalKO;
    private final String notifySubjectOK;
    private final String notifySubjectKO;
    private static final String MARKDOWN_NA = "n.a.";
    private final Integer notifyUpdateRows;
    private final AwardWinnerErrorDAO awardWinnerErrorDAO;
    private static final String ORDINE_OK = "ORDINE ESEGUITO";
    private final List<String> notifyResultList;

    private static final Map<Long, String> phases = new HashMap();
    private static final Map<Long, String> awPeriods = new HashMap();

    static {
        phases.put(1L, "primo");
        phases.put(2L, "secondo");
        phases.put(3L, "terzo");
        phases.put(4L, "quarto");
        phases.put(5L, "quinto");
        phases.put(6L, "sesto");
        phases.put(7L, "settimo");
        phases.put(8L, "ottavo");
        phases.put(9L, "nono");
        phases.put(10L, "decimo");
    }

    static {
        awPeriods.put(1L, "primo");
        awPeriods.put(2L, "secondo");
        awPeriods.put(3L, "terzo");
        awPeriods.put(4L, "quarto");
        awPeriods.put(5L, "quinto");
        awPeriods.put(6L, "sesto");
        awPeriods.put(7L, "settimo");
        awPeriods.put(8L, "ottavo");
        awPeriods.put(9L, "nono");
        awPeriods.put(10L, "decimo");
    }

    private final CitizenRankingDAO citizenRankingDAO;
    private final Long notifyEndPeriodLimit;
    private final Long notifyEndPeriodOffset;
    private final String subjectEndPeriod;
    private final String markdownEndPeriod;
    private final String subjectEndGracePeriodOK;
    private final String markdownEndGracePeriodOK;
    private final String markdownEndGracePeriodOKSuperCash;
    private final String subjectEndGracePeriodKO;
    private final String markdownEndGracePeriodKO;

    @Autowired
    NotificationIOServiceImpl(
            CitizenDAO citizenDAO,
            NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyWinners.maxRow}") Long maxNotifyRow,
            @Value("${core.NotificationService.notifyWinners.maxNotifyTry}") Long maxNotifyTimes,
            @Value("${core.NotificationService.notifyWinners.markdown.ok}") String notifyMarkdownOK,
            @Value("${core.NotificationService.notifyWinners.markdown.ok.tech}") String notifyMarkdownTechnicalOK,
            @Value("${core.NotificationService.notifyWinners.markdown.ko}") String notifyMarkdownKO,
            @Value("${core.NotificationService.notifyWinners.markdown.ko.tech}") String notifyMarkdownTechnicalKO,
            @Value("${core.NotificationService.notifyWinners.subject.ok}") String notifySubjectOK,
            @Value("${core.NotificationService.notifyWinners.subject.ko}") String notifySubjectKO,
            @Value("${core.NotificationService.notifyWinners.updateRowsNumber}") Integer notifyUpdateRows,
            AwardWinnerErrorDAO awardWinnerErrorDAO,
            @Value("${core.NotificationService.notifyWinners.resultList}") List<String> notifyResultList,
            CitizenRankingDAO citizenRankingDAO,
            @Value("${core.NotificationService.notify.end.period.offset}") Long notifyEndPeriodOffset,
            @Value("${core.NotificationService.notify.end.period.limit}") Long notifyEndPeriodLimit,
            @Value("${core.NotificationService.notify.end.period.subject}") String subjectEndPeriod,
            @Value("${core.NotificationService.notify.end.period.markdown}") String markdownEndPeriod,
            @Value("${core.NotificationService.notify.end.grace.period.subject.ok}") String subjectEndGracePeriodOK,
            @Value("${core.NotificationService.notify.end.grace.period.markdown.ok.supercashback}") String markdownEndGracePeriodOKSuperCash,
            @Value("${core.NotificationService.notify.end.grace.period.markdown.ok}") String markdownEndGracePeriodOK,
            @Value("${core.NotificationService.notify.end.grace.period.subject.ko}") String subjectEndGracePeriodKO,
            @Value("${core.NotificationService.notify.end.grace.period.markdown.ko}") String markdownEndGracePeriodKO
    ) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.timeToLive = timeToLive;
        this.maxNotifyRow = maxNotifyRow;
        this.maxNotifyTimes = maxNotifyTimes != null ? maxNotifyTimes : -1L;
        this.notifyMarkdownOK = notifyMarkdownOK;
        this.notifyMarkdownTechnicalOK = notifyMarkdownTechnicalOK;
        this.notifyMarkdownKO = notifyMarkdownKO;
        this.notifySubjectOK = notifySubjectOK;
        this.notifySubjectKO = notifySubjectKO;
        this.notifyMarkdownTechnicalKO = notifyMarkdownTechnicalKO;
        this.notifyUpdateRows = notifyUpdateRows;
        this.awardWinnerErrorDAO = awardWinnerErrorDAO;
        this.notifyResultList = notifyResultList;
        this.citizenRankingDAO = citizenRankingDAO;
        this.subjectEndPeriod = subjectEndPeriod;
        this.markdownEndPeriod = markdownEndPeriod;
        this.subjectEndGracePeriodOK = subjectEndGracePeriodOK;
        this.markdownEndGracePeriodOK = markdownEndGracePeriodOK;
        this.markdownEndGracePeriodOKSuperCash = markdownEndGracePeriodOKSuperCash;
        this.subjectEndGracePeriodKO = subjectEndGracePeriodKO;
        this.markdownEndGracePeriodKO = markdownEndGracePeriodKO;
        this.notifyEndPeriodOffset = notifyEndPeriodOffset;
        this.notifyEndPeriodLimit = notifyEndPeriodLimit;
    }

    @Override
    @Transactional
    public int notifyWinnersPayments() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments start");
        }
        int notityCount = 0;
        int errorCount = 0;
        List<AwardWinnerError> errorRecords = new ArrayList<AwardWinnerError>();

        List<WinningCitizen> winners = citizenDAO.findWinnersToNotify(-1L, maxNotifyTimes, notifyResultList, 0L, maxNotifyRow);

        for (WinningCitizen toNotifyWin : winners) {
            try {
                //                if (notificationRestConnector.profiles(toNotifyWin.getFiscalCode()) != null) {
                String notifyMarkdown = getNotifyMarkdown(toNotifyWin);
                String notifySubject = getNotifySubject(toNotifyWin);

                NotificationResource resource = this.sendNotifyIO(toNotifyWin.getFiscalCode(), notifySubject, notifyMarkdown);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes() != null ?
                        toNotifyWin.getNotifyTimes() : 0L, 1L));
                toNotifyWin.setNotifyId(resource.getId());

                notityCount += 1;
                //                }
                toNotifyWin.setToNotify(Boolean.FALSE);

            } catch (NotifyTooManyRequestException e) {
                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes() != null ?
                        toNotifyWin.getNotifyTimes() : 0L, 1L));
            } catch (FeignException e) {
                try {
                    if (log.isErrorEnabled()) {
                        log.error(e.contentUTF8());
                    }
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error(ex.getMessage());
                    }
                }

                AwardWinnerError winnerError = getErrorRow(toNotifyWin, e);

                awardWinnerErrorDAO.save(winnerError);
                errorCount += 1;

                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes() != null ?
                        toNotifyWin.getNotifyTimes() : 0L, 1L));
            } catch (Exception e) {
                if (log.isErrorEnabled() && e != null) {
                    log.error(e.getMessage());
                }

                AwardWinnerError winnerError = getErrorRow(toNotifyWin, e);

                awardWinnerErrorDAO.save(winnerError);
                errorCount += 1;

                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes() != null ?
                        toNotifyWin.getNotifyTimes() : 0L, 1L));
            }

            toNotifyWin.setUpdateUser("notifyWinnersPayments");
            toNotifyWin.setUpdateDate(OffsetDateTime.now());

            citizenDAO.save(toNotifyWin);
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments end " +
                    "- Notified " + notityCount + " citizens"
                    + (errorCount != 0 ? " - Errors: " + errorCount + " record" : ""));
        }

        return winners != null && !winners.isEmpty() ? winners.size() : 0;
    }

    private String getNotifyMarkdown(WinningCitizen toNotifyWin) {
        String retVal = null;
        if (ORDINE_OK.equals(toNotifyWin.getEsitoBonifico())) {
            if (toNotifyWin.getTechnicalAccountHolder() == null || toNotifyWin.getTechnicalAccountHolder().isEmpty()) {
                retVal = this.notifyMarkdownOK.replace("{{amount}}", toNotifyWin.getAmount() != null ? toNotifyWin.getAmount().setScale(2, ROUND_HALF_DOWN).toString().replace(".", ",") : MARKDOWN_NA)
                        .replace("{{executionDate}}", toNotifyWin.getBankTransferDate() != null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                        .replace("{{cro}}", toNotifyWin.getCro() != null ? toNotifyWin.getCro() : MARKDOWN_NA)
                        .replace("{{IBAN}}", toNotifyWin.getPayoffInstr())
                        .replace("{{startDate}}", toNotifyWin.getAwardPeriodStart().format(ONLY_DATE_FORMATTER))
                        .replace("{{endDate}}", toNotifyWin.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER));
            } else {
                retVal = this.notifyMarkdownTechnicalOK.replace("{{amount}}", toNotifyWin.getAmount() != null ? toNotifyWin.getAmount().setScale(2, ROUND_HALF_DOWN).toString().replace(".", ",") : MARKDOWN_NA)
                        .replace("{{executionDate}}", toNotifyWin.getBankTransferDate() != null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                        .replace("{{cro}}", toNotifyWin.getCro() != null ? toNotifyWin.getCro() : MARKDOWN_NA)
                        .replace("{{startDate}}", toNotifyWin.getAwardPeriodStart().format(ONLY_DATE_FORMATTER))
                        .replace("{{endDate}}", toNotifyWin.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER));
            }
        } else {
            if (toNotifyWin.getTechnicalAccountHolder() == null || toNotifyWin.getTechnicalAccountHolder().isEmpty()) {
                retVal = this.notifyMarkdownKO.replace("{{amount}}", toNotifyWin.getAmount() != null ? toNotifyWin.getAmount().setScale(2, ROUND_HALF_DOWN).toString().replace(".", ",") : MARKDOWN_NA)
                        .replace("{{executionDate}}", toNotifyWin.getBankTransferDate() != null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                        .replace("{{resultReason}}", toNotifyWin.getResultReason() != null ? toNotifyWin.getResultReason() : MARKDOWN_NA)
                        .replace("{{cro}}", toNotifyWin.getCro() != null ? toNotifyWin.getCro() : MARKDOWN_NA)
                        .replace("{{IBAN}}", toNotifyWin.getPayoffInstr());
            } else {
                retVal = this.notifyMarkdownTechnicalKO.replace("{{amount}}", toNotifyWin.getAmount() != null ? toNotifyWin.getAmount().setScale(2, ROUND_HALF_DOWN).toString().replace(".", ",") : MARKDOWN_NA)
                        .replace("{{executionDate}}", toNotifyWin.getBankTransferDate() != null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                        .replace("{{resultReason}}", toNotifyWin.getResultReason() != null ? toNotifyWin.getResultReason() : MARKDOWN_NA)
                        .replace("{{cro}}", toNotifyWin.getCro() != null ? toNotifyWin.getCro() : MARKDOWN_NA);
            }

        }
        return retVal.replace("\\n", System.lineSeparator());
    }

    private String getNotifySubject(WinningCitizen toNotifyWin) {
        return ORDINE_OK.equals(toNotifyWin.getEsitoBonifico()) ? this.notifySubjectOK : this.notifySubjectKO;
    }

    private AwardWinnerError getErrorRow(WinningCitizen toNotifyWin, Exception e) {
        AwardWinnerError winnerError = new AwardWinnerError();
        String errorMessage = null;
        String errocCode = null;

        try {
            if (e.getCause() != null
                    && e.getCause() instanceof FeignException) {
                errocCode = ((FeignException) e.getCause()).contentUTF8();
                errorMessage = String.valueOf(((FeignException) e.getCause()).status());
            }
        } catch (Exception ex) {
            errocCode = "ErrorContentUTF8 is null";
            errorMessage = "GenericError";
        }

        winnerError.setId(toNotifyWin.getId());
        winnerError.setFiscalCode(toNotifyWin.getFiscalCode());
        winnerError.setAwardPeriodId(toNotifyWin.getAwardPeriodId());
        winnerError.setErrorCode(errocCode != null ? errocCode : "500");
        winnerError.setErrorMessage(errorMessage != null ? errorMessage : e.getMessage());
        winnerError.setEnabled(Boolean.TRUE);
        winnerError.setInsertUser("notifyWinnersPayments");
        winnerError.setInsertDate(OffsetDateTime.now());

        return winnerError;
    }

    private NotificationResource sendNotifyIO(String fiscalCode, String notifySubject, String notifyMarkdown) {
        NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                fiscalCode, timeToLive, notifySubject, notifyMarkdown);
        return notificationRestConnector.notify(dto);
    }

    @Override
    public void notifyEndPeriodOrEndGracePeriod(AwardPeriod awardPeriod, Boolean isEndPeriod) throws IOException {

        Long limit = notifyEndPeriodLimit;

        String notifySubject = subjectEndPeriod;
        String notifyMarkdown = markdownEndPeriod;

        String step = null;
        int errorNot = 0;

        List<CitizenRanking> citizenToNotify = null;

        do {
            if (isEndPeriod) {
                step = "END_PERIOD_" + awardPeriod.getAwardPeriodId().toString();
            } else {
                step = "END_GRACE_PERIOD_" + awardPeriod.getAwardPeriodId().toString();
            }
            log.info("RECORD PER LA QUERY awPerId: " + awardPeriod.getAwardPeriodId() + " step: " + step + "aw_end_date: " + awardPeriod.getEndDate());
            citizenToNotify = citizenRankingDAO
                    .extractRankingByAwardPeriodOrderByTransactionFiscalCode(
                            awardPeriod.getAwardPeriodId(), step, notifyEndPeriodLimit, awardPeriod.getEndDate());
            log.info("RECORD ESTRATTISUL PERIODO 2: " + citizenToNotify.size());

            for (CitizenRanking citRanking : citizenToNotify) {
                Boolean updateCit = Boolean.TRUE;

                if (!isEndPeriod) {
                    if (citRanking.getRanking() != null
                            && citRanking.getTransactionNumber().compareTo(awardPeriod.getMinTransactionNumber()) > -1) {
                        notifySubject = subjectEndGracePeriodOK;

                        if (awardPeriod.getMinPosition().compareTo(citRanking.getRanking()) > -1) {
                            notifyMarkdown = markdownEndGracePeriodOKSuperCash;
                        } else {
                            notifyMarkdown = markdownEndGracePeriodOK;
                        }
                    } else {
                        notifySubject = subjectEndGracePeriodKO;
                        notifyMarkdown = markdownEndGracePeriodKO;
                    }
                    step = "END_GRACE_PERIOD_" + awardPeriod.getAwardPeriodId().toString();
                } else {
                    step = "END_PERIOD_" + awardPeriod.getAwardPeriodId().toString();
                }

                try {
                    notifySubject = notifySubject.replace("{{award_period}}", awPeriods.get(awardPeriod.getAwardPeriodId()));
                    notifyMarkdown = notifyMarkdown
                            .replace("{{phase}}", phases.get(awardPeriod.getAwardPeriodId()))
                            .replace("{{maxTransaction}}", awardPeriod.getMaxTransactionCashback().toString())
                            .replace("{{amount}}", citRanking.getTotalCashback() != null ? citRanking.getTotalCashback().setScale(2, ROUND_HALF_DOWN).toString().replace(".", ",") : MARKDOWN_NA)
                            .replace("\\n", System.lineSeparator());

                    sendNotifyIO(citRanking.getFiscalCode(), notifySubject, notifyMarkdown);
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error("Unable to send notify to citizen");
                    }
                    step = step + "_ERROR";
                    errorNot++;
                }

                citizenDAO.updateCitizenWithNotificationStep(citRanking.getFiscalCode(), step);
            }

            if (logger.isInfoEnabled()) {
                logger.info("NotificationIOServiceImpl.notifyEndPeriodOrEndGracePeriod - Sended notifies to " + citizenToNotify.size() + " citizens with " + errorNot + " errors");
            }

            errorNot = 0;

        } while (citizenToNotify != null && !citizenToNotify.isEmpty());
    }
}
