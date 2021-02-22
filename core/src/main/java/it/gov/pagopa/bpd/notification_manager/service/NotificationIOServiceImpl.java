package it.gov.pagopa.bpd.notification_manager.service;

import eu.sia.meda.service.BaseService;
import feign.FeignException;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
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
import org.apache.commons.beanutils.locale.converters.BigDecimalLocaleConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NotificationIOServiceImpl extends BaseService implements NotificationIOService{

    private static final DateTimeFormatter ONLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final Long timeToLive;
    private final Long maxNotifyRow;
    private final Long maxNotifyTimes;
    private final String notifyMarkdownOK;
    private final String notifyMarkdownKO;
    private final String notifySubjectOK;
    private final String notifySubjectKO;
    private final Integer notifyUpdateRows;
    private final AwardWinnerErrorDAO awardWinnerErrorDAO;
    private static final String MARKDOWN_NA="n.a.";
    private static final String ORDINE_OK="ORDINE ESEGUITO";
    private final List<String> notifyResultList;

    @Autowired
    NotificationIOServiceImpl(
            CitizenDAO citizenDAO,
            NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyWinners.maxRow}") Long maxNotifyRow,
            @Value("${core.NotificationService.notifyWinners.maxNotifyTry}") Long maxNotifyTimes,
            @Value("${core.NotificationService.notifyWinners.markdown.ok}") String notifyMarkdownOK,
            @Value("${core.NotificationService.notifyWinners.markdown.ko}") String notifyMarkdownKO,
            @Value("${core.NotificationService.notifyWinners.subject.ok}") String notifySubjectOK,
            @Value("${core.NotificationService.notifyWinners.subject.ko}") String notifySubjectKO,
            @Value("${core.NotificationService.notifyWinners.updateRowsNumber}") Integer notifyUpdateRows,
            AwardWinnerErrorDAO awardWinnerErrorDAO,
            @Value("${core.NotificationService.notifyWinners.resultList}") List<String> notifyResultList
    ) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.timeToLive = timeToLive;
        this.maxNotifyRow=maxNotifyRow;
        this.maxNotifyTimes=maxNotifyTimes!=null ? maxNotifyTimes : -1L;
        this.notifyMarkdownOK=notifyMarkdownOK;
        this.notifyMarkdownKO=notifyMarkdownKO;
        this.notifySubjectOK=notifySubjectOK;
        this.notifySubjectKO=notifySubjectKO;
        this.notifyUpdateRows=notifyUpdateRows;
        this.awardWinnerErrorDAO=awardWinnerErrorDAO;
        this.notifyResultList=notifyResultList;
    }

    @Override
    @Transactional
    public int notifyWinnersPayments() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments start");
        }
        int notityCount = 0;
        int errorCount = 0;
        List<AwardWinnerError> errorRecords=new ArrayList<AwardWinnerError>();

        List<WinningCitizen> winners = citizenDAO.findWinnersToNotify(-1L, maxNotifyTimes, notifyResultList,0L, maxNotifyRow);

        for(WinningCitizen toNotifyWin : winners){
            try {
                //                if (notificationRestConnector.profiles(toNotifyWin.getFiscalCode()) != null) {
                String notifyMarkdown = getNotifyMarkdown(toNotifyWin);
                String notifySubject = getNotifySubject(toNotifyWin);

                NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                        toNotifyWin.getFiscalCode(), timeToLive, notifySubject, notifyMarkdown);
                NotificationResource resource = notificationRestConnector.notify(dto);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes()!=null?
                        toNotifyWin.getNotifyTimes() : 0L,1L));
                toNotifyWin.setNotifyId(resource.getId());

                notityCount += 1;
                //                }
                toNotifyWin.setToNotify(Boolean.FALSE);

            }catch(NotifyTooManyRequestException e){
                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes()!=null?
                        toNotifyWin.getNotifyTimes() : 0L,1L));
            }catch(FeignException e){
                if(log.isErrorEnabled()){
                    log.error(e.contentUTF8());
                }

                AwardWinnerError winnerError = new AwardWinnerError();
                winnerError.setId(toNotifyWin.getId());
                winnerError.setFiscalCode(toNotifyWin.getFiscalCode());
                winnerError.setAwardPeriodId(toNotifyWin.getAwardPeriodId());
                winnerError.setErrorCode(String.valueOf(e.status()));
                winnerError.setErrorMessage(e.contentUTF8());
                winnerError.setEnabled(Boolean.TRUE);
                winnerError.setInsertUser("notifyWinnersPayments");
                winnerError.setInsertDate(OffsetDateTime.now());

                awardWinnerErrorDAO.save(winnerError);
                errorCount+=1;

                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes()!=null?
                        toNotifyWin.getNotifyTimes() : 0L,1L));
            }

            toNotifyWin.setUpdateUser("notifyWinnersPayments");
            toNotifyWin.setUpdateDate(OffsetDateTime.now());

            citizenDAO.save(toNotifyWin);
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments end " +
                    "- Notified " + notityCount + " citizens"
                    +(errorCount!=0 ? " - Errors: " + errorCount +" record" : ""));
        }

        return winners!=null && !winners.isEmpty() ? winners.size():0;
    }

    private String getNotifyMarkdown(WinningCitizen toNotifyWin){
        String retVal= null;
        if(ORDINE_OK.equals(toNotifyWin.getEsitoBonifico())){
            retVal=this.notifyMarkdownOK.replace("{{amount}}",toNotifyWin.getAmount()!=null ? NumberFormat.getInstance().format(toNotifyWin.getAmount()) : MARKDOWN_NA)
                    .replace("{{executionDate}}",toNotifyWin.getBankTransferDate()!=null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                    .replace("{{cro}}",toNotifyWin.getCro()!=null ? toNotifyWin.getCro() : MARKDOWN_NA)
                    .replace("{{IBAN}}",toNotifyWin.getPayoffInstr())
                    .replace("{{startDate}}", toNotifyWin.getAwardPeriodStart().format(ONLY_DATE_FORMATTER))
                    .replace("{{endDate}}", toNotifyWin.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER));
        }else{
            retVal=this.notifyMarkdownKO.replace("{{amount}}",toNotifyWin.getAmount()!=null ? NumberFormat.getInstance().format(toNotifyWin.getAmount()) : MARKDOWN_NA)
                    .replace("{{executionDate}}",toNotifyWin.getBankTransferDate()!=null ? toNotifyWin.getBankTransferDate().format(ONLY_DATE_FORMATTER) : MARKDOWN_NA)
                    .replace("{{resultReason}}",toNotifyWin.getResultReason()!=null ? toNotifyWin.getResultReason() : MARKDOWN_NA)
                    .replace("{{cro}}",toNotifyWin.getCro()!=null ? toNotifyWin.getCro() : MARKDOWN_NA);
        }
        return retVal.replace("\\n",System.lineSeparator());
    }

    private String getNotifySubject(WinningCitizen toNotifyWin){
        return ORDINE_OK.equals(toNotifyWin.getEsitoBonifico()) ? this.notifySubjectOK : this.notifySubjectKO;
    }
}
