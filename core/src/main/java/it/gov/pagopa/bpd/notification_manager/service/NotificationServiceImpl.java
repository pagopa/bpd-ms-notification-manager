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
            @Value("${core.NotificationService.notifyWinners.maxRow}") Long maxNotifyRow,
            @Value("${core.NotificationService.notifyWinners.maxNotifyTry}") Long maxNotifyTimes,
            @Value("${core.NotificationService.notifyWinners.markdown.ok}") String notifyMarkdownOK,
            @Value("${core.NotificationService.notifyWinners.markdown.ko}") String notifyMarkdownKO,
            @Value("${core.NotificationService.notifyWinners.subject.ok}") String notifySubjectOK,
            @Value("${core.NotificationService.notifyWinners.subject.ko}") String notifySubjectKO,
            @Value("${core.NotificationService.notifyWinners.updateRowsNumber}") Integer notifyUpdateRows,
            AwardWinnerErrorDAO awardWinnerErrorDAO
            ) {
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
        this.maxNotifyRow=maxNotifyRow;
        this.maxNotifyTimes=maxNotifyTimes!=null ? maxNotifyTimes : -1L;
        this.notifyMarkdownOK=notifyMarkdownOK;
        this.notifyMarkdownKO=notifyMarkdownKO;
        this.notifySubjectOK=notifySubjectOK;
        this.notifySubjectKO=notifySubjectKO;
        this.notifyUpdateRows=notifyUpdateRows;
        this.awardWinnerErrorDAO=awardWinnerErrorDAO;
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
    @Transactional
    public void notifyWinnersPayments() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments start");
        }
        int updateCount = 0;
        int notityCount = 0;
        int errorCount = 0;

        List<WinningCitizen> winners = citizenDAO.findWinnersToNotify(-1L, maxNotifyTimes,0L, maxNotifyRow);

        List<WinningCitizen> updateRecords=new ArrayList<WinningCitizen>();
        List<AwardWinnerError> errorRecords=new ArrayList<AwardWinnerError>();

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

                errorRecords.add(winnerError);
                errorCount+=1;

                toNotifyWin.setToNotify(Boolean.TRUE);
                toNotifyWin.setNotifyTimes(Long.sum(toNotifyWin.getNotifyTimes()!=null?
                        toNotifyWin.getNotifyTimes() : 0L,1L));
            }

            toNotifyWin.setUpdateUser("notifyWinnersPayments");
            toNotifyWin.setUpdateDate(OffsetDateTime.now());

            updateRecords.add(toNotifyWin);
            updateCount+=1;

            if(updateCount==this.notifyUpdateRows){
                citizenDAO.saveAll(updateRecords);
                updateRecords.clear();
                updateCount=0;
            }
        }
        if(!updateRecords.isEmpty()) {
            citizenDAO.saveAll(updateRecords);
        }
        if(!errorRecords.isEmpty()){
            awardWinnerErrorDAO.saveAll(errorRecords);
        }
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.notifyWinnersPayments end " +
                    "- Notified " + notityCount + " citizens"
                    +(errorCount!=0 ? " - Errors: " + errorCount +" record" : ""));
        }
    }

    private String getNotifyMarkdown(WinningCitizen toNotifyWin){
        String retVal= null;
        if(ORDINE_OK.equals(toNotifyWin.getEsitoBonifico())){
            retVal=this.notifyMarkdownOK.replace("{{amount}}",toNotifyWin.getAmount().toString()!=null ? toNotifyWin.getAmount().toString() : MARKDOWN_NA)
                            .replace("{{executionDate}}",toNotifyWin.getBankTransferDate()!=null ? toNotifyWin.getBankTransferDate().format(DateTimeFormatter.ISO_DATE) : MARKDOWN_NA)
                            .replace("{{cro}}",toNotifyWin.getCro()!=null ? toNotifyWin.getCro() : MARKDOWN_NA);
        }else{
            retVal=this.notifyMarkdownKO.replace("{{amount}}",toNotifyWin.getAmount().toString()!=null ? toNotifyWin.getAmount().toString() : MARKDOWN_NA)
                            .replace("{{resultReason}}",toNotifyWin.getResultReason()!=null ? toNotifyWin.getResultReason() : MARKDOWN_NA)
                            .replace("{{cro}}",toNotifyWin.getCro()!=null ? toNotifyWin.getCro() : MARKDOWN_NA);
        }
        return retVal.replace("\\n",System.lineSeparator());
    }

    private String getNotifySubject(WinningCitizen toNotifyWin){
        return ORDINE_OK.equals(toNotifyWin.getEsitoBonifico()) ? this.notifySubjectOK : this.notifySubjectKO;
    }
}

