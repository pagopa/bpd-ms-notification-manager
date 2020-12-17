package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.WinnersSftpConnector;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/notificationService.properties")
@ContextConfiguration(classes = NotificationServiceImpl.class)
public class NotificationServiceImplTest {


    @MockBean
    private NotificationRestConnector restConnector;

    @MockBean
    private CitizenDAO citizenDAOMock;

    @SpyBean
    private NotificationDtoMapper notificationDtoMapper;

    @Autowired
    private NotificationServiceImpl notificationService;

    @MockBean
    private AwardPeriodRestClient awardPeriodRestClientMock;

    @MockBean
    private WinnersSftpConnector winnersSftpConnectorMock;

    @PostConstruct
    public void configureMock() {
        BDDMockito.when(restConnector.notify(Mockito.any(NotificationDTO.class)))
                .thenAnswer(invocation -> {
                    NotificationResource result = new NotificationResource();
                    result.setId("ok");
                    return result;
                });
        BDDMockito.when(citizenDAOMock.findFiscalCodesWithUnsetPayoffInstr())
                .thenAnswer(invocation -> {
                    ArrayList<String> result = new ArrayList<>();
                    result.add("CF1");
                    result.add("CF2");
                    result.add("CF3");
                    return result;
                });

        BDDMockito.when(citizenDAOMock.findWinners(Mockito.any(Long.class)))
                .thenAnswer(invocation -> {
                    List<WinningCitizen> result = new ArrayList<>();
                    WinningCitizen winner1 = new WinningCitizen();
                    winner1.setAwardPeriodId(2L);
                    winner1.setPayoffInstr("test");
                    winner1.setFiscalCode("test");
                    winner1.setAccountHolderFiscalCode("test");
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
                    return result;
                });
    }

    @Test
    public void testFindFiscalCodesWithUnsetPayoffInstr() {

        notificationService.notifyUnsetPayoffInstr();

        verify(citizenDAOMock, only()).findFiscalCodesWithUnsetPayoffInstr();
        verify(restConnector, times(3))
                .notify(Mockito.any(NotificationDTO.class));
    }

    @Test
    public void testUpdateRanking() throws IOException {

        notificationService.updateRankingAndWinners();
        verify(citizenDAOMock, only()).updateRankingAndWinners();
        verify(citizenDAOMock, times(1)).updateRankingAndWinners();
    }

    @Test
    public void testFindWinnersEndingPeriod(){

        BDDMockito.when(awardPeriodRestClientMock.findActiveAwardPeriods())
                .thenAnswer(invocation -> {
                    List<AwardPeriod> result = new ArrayList<>();
                    AwardPeriod awp1 = new AwardPeriod();
                    awp1.setAwardPeriodId(2L);
                    awp1.setEndDate(LocalDate.now().minus(Period.ofDays(15)));
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

        notificationService.findWinners();

        verify(citizenDAOMock).findWinners(2L);
        verify(winnersSftpConnectorMock, only()).sendFile(Mockito.any(File.class));
        verify(awardPeriodRestClientMock, only()).findActiveAwardPeriods();

    }

    @Test
    public void testFindWinners(){

        BDDMockito.when(awardPeriodRestClientMock.findActiveAwardPeriods())
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

        notificationService.findWinners();

        verifyZeroInteractions(citizenDAOMock, winnersSftpConnectorMock);
        verify(awardPeriodRestClientMock, only()).findActiveAwardPeriods();

    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    static {
        try {
            String publicKey = readFile("src/test/resources/test_pgp/test", StandardCharsets.US_ASCII);
            System.setProperty("NOTIFICATION_SERVICE_NOTIFY_WINNERS_PUBLIC_KEY", publicKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}