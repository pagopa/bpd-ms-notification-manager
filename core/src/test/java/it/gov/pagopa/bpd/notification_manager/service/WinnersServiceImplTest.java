package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.WinnersSftpConnector;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.jdbc.CitizenJdbcDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningCitizenDto;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.AwardWinnerErrorDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.exception.UpdateWinnerStatusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/notificationService.properties",
        properties = {"core.NotificationService.findWinners.maxRow=5", "core.NotificationService.findWinners.deleteTmpFiles.enable=true"})
@ContextConfiguration(classes = {WinnersServiceImpl.class})
public class WinnersServiceImplTest {

    @MockBean
    private AwardPeriodRestClient awardPeriodRestClientMock;
    @MockBean
    private CitizenJdbcDAO citizenJdbcDAOMock;

    static {
        try {
            String publicKey = readFile("src/test/resources/test_pgp/test", StandardCharsets.US_ASCII);
            System.setProperty("NOTIFICATION_SERVICE_NOTIFY_WINNERS_PUBLIC_KEY", publicKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MockBean
    private CitizenDAO citizenDAOMock;
    @MockBean
    private AwardWinnerErrorDAO awardWinnerErrorDAO;
    @MockBean
    private WinnersSftpConnector winnersSftpConnectorMock;
    private Error error;
    private MissRecord missRecords;
    @Autowired
    private WinnersServiceImpl winnersService;

    @PostConstruct
    public void configureMock() {
        BDDMockito.when(citizenDAOMock.findWinners(Mockito.any(Long.class), Mockito.any(Long.class)))
                .thenAnswer(invocation -> {
                    Long apId = invocation.getArgument(0, Long.class);
                    List<WinningCitizen> result = new ArrayList<>();

                    if (apId >= 0) {
                        WinningCitizen winner1 = new WinningCitizen();
                        winner1.setAwardPeriodId(2L);
                        winner1.setPayoffInstr("test");
                        winner1.setFiscalCode("test");
                        winner1.setAccountHolderFiscalCode("test-different");
                        winner1.setAccountHolderName("test");
                        winner1.setAccountHolderSurname("test");
                        winner1.setCheckInstrStatus("01");
                        winner1.setId(1L);
                        winner1.setAwardPeriodStart(LocalDate.now());
                        winner1.setAwardPeriodEnd(LocalDate.now());
                        winner1.setAmount(new BigDecimal("124567890.0987654321"));
                        winner1.setCashback(new BigDecimal("124567890.0987654321"));
                        winner1.setJackpot(new BigDecimal("124567890.0987654321"));
                        winner1.setTypology("01");
                        result.add(winner1);
                    }

                    return result;
                });

        BDDMockito.when(citizenJdbcDAOMock.updateWinningCitizenStatus(Mockito.any()))
                .thenAnswer(invocationOnMock -> {
                    Collection<WinningCitizenDto> rankings = invocationOnMock.getArgument(0, Collection.class);
                    int size = rankings.size();
                    if (!rankings.isEmpty() && Error.UPDATE_WINNER_STATUS == error) {
                        size--;
                    }
                    int[] result = new int[size];
                    Arrays.fill(result, 1);
                    if (!rankings.isEmpty() && MissRecord.UPDATE_WINNER_STATUS == missRecords) {
                        result[0] = 0;
                    }
                    return result;
                });

        BDDMockito.when(citizenJdbcDAOMock.updateWinningCitizenStatusAndFilename(Mockito.any()))
                .thenAnswer(invocationOnMock -> {
                    Collection<WinningCitizenDto> rankings = invocationOnMock.getArgument(0, Collection.class);
                    int size = rankings.size();
                    if (!rankings.isEmpty() && Error.UPDATE_WINNER_STATUS == error) {
                        size--;
                    }
                    int[] result = new int[size];
                    Arrays.fill(result, 1);
                    if (!rankings.isEmpty() && MissRecord.UPDATE_WINNER_STATUS == missRecords) {
                        result[0] = 0;
                    }
                    return result;
                });
    }

    @Before
    public void init() {
        error = null;
        missRecords = null;
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    @Test
    public void testSendWinnersWithResults() {
        winnersService.sendWinners(0L, null);

        verify(citizenDAOMock, atLeastOnce()).findWinners(Mockito.any(Long.class), Mockito.any(Long.class));
        verify(winnersSftpConnectorMock, atLeastOnce()).sendFile(Mockito.any(File.class));
    }

    @Test
    public void testSendWinnersWithoutResults() {
        winnersService.sendWinners(-1L, null);

        verifyZeroInteractions(winnersSftpConnectorMock);
    }

    @Test(expected = UpdateWinnerStatusException.class)
    public void testSendWinnersWithUpdateStatusError() {
        error = Error.UPDATE_WINNER_STATUS;

        try {
            winnersService.sendWinners(0L, null);

        } catch (Exception e) {
            verify(citizenDAOMock, atLeastOnce()).findWinners(Mockito.any(Long.class), Mockito.any(Long.class));
            verify(winnersSftpConnectorMock, never()).sendFile(Mockito.any(File.class));

            throw e;
        }
    }

    @Test(expected = UpdateWinnerStatusException.class)
    public void testSendWinnersWithUpdateStatusMissedRecords() {
        missRecords = MissRecord.UPDATE_WINNER_STATUS;

        try {
            winnersService.sendWinners(0L, null);

        } catch (Exception e) {
            verify(citizenDAOMock, atLeastOnce()).findWinners(Mockito.any(Long.class), Mockito.any(Long.class));
            verify(winnersSftpConnectorMock, never()).sendFile(Mockito.any(File.class));

            throw e;
        }
    }

    @Test
    public void testUpdateWinners() {
        winnersService.updateWinners(Mockito.anyLong());
        verify(citizenDAOMock, only()).updateWinners(Mockito.anyLong());
        verify(citizenDAOMock, times(1)).updateWinners(Mockito.anyLong());
    }

    @Test
    public void testSendWinnersEndingPeriod() throws IOException {
        BDDMockito.when(awardPeriodRestClientMock.findAllAwardPeriods())
                .thenAnswer(invocation -> {
                    List<AwardPeriod> result = new ArrayList<>();
                    AwardPeriod awp1 = new AwardPeriod();
                    awp1.setAwardPeriodId(2L);
                    awp1.setEndDate(LocalDate.now().minus(Period.ofDays(15)));
                    awp1.setGracePeriod(14L);
                    awp1.setStartDate(LocalDate.now().minus(Period.ofDays(50)));
                    result.add(awp1);
                    AwardPeriod awp2 = new AwardPeriod();
                    awp2.setAwardPeriodId(1L);
                    awp2.setEndDate(LocalDate.now().minus(Period.ofDays(5)));
                    awp2.setGracePeriod(15L);
                    awp2.setStartDate(LocalDate.now().minus(Period.ofDays(40)));
                    result.add(awp2);
                    return result;
                });

        winnersService.updateAndSendWinners();

//        verify(winnersService, atLeastOnce()).sendWinners(Mockito.any(Long.class), Mockito.anyInt(), Mockito.any(), Mockito.any(LocalDateTime.class), Mockito.anyInt());
        verify(awardPeriodRestClientMock, only()).findAllAwardPeriods();
    }

    @Test
    public void testSendWinners() throws IOException {
        BDDMockito.when(awardPeriodRestClientMock.findAllAwardPeriods())
                .thenAnswer(invocation -> {
                    List<AwardPeriod> result = new ArrayList<>();
                    AwardPeriod awp1 = new AwardPeriod();
                    awp1.setAwardPeriodId(2L);
                    awp1.setEndDate(LocalDate.now());
                    awp1.setGracePeriod(15L);
                    awp1.setStartDate(LocalDate.now().minus(Period.ofDays(50)));
                    result.add(awp1);
                    AwardPeriod awp2 = new AwardPeriod();
                    awp2.setAwardPeriodId(1L);
                    awp2.setEndDate(LocalDate.now().minus(Period.ofDays(5)));
                    awp2.setGracePeriod(15L);
                    awp2.setStartDate(LocalDate.now().minus(Period.ofDays(40)));
                    result.add(awp2);
                    return result;
                });

        winnersService.updateAndSendWinners();

//        verifyZeroInteractions(winnersService);
        verify(awardPeriodRestClientMock, only()).findAllAwardPeriods();
    }


    private enum Error {
        UPDATE_WINNER_STATUS
    }


    private enum MissRecord {
        UPDATE_WINNER_STATUS
    }

}