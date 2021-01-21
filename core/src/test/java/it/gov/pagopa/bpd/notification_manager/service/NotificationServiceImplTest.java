package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/notificationService.properties")
@ContextConfiguration(classes = {NotificationServiceImpl.class})
public class NotificationServiceImplTest {

    @MockBean
    private NotificationRestConnector restConnector;

    @MockBean
    private CitizenDAO citizenDAOMock;

    @SpyBean
    private NotificationDtoMapper notificationDtoMapper;

    @MockBean
    private WinnersService winnersService;

    @Autowired
    private NotificationServiceImpl notificationService;

    @MockBean
    private AwardPeriodRestClient awardPeriodRestClientMock;


    @Test
    public void testFindFiscalCodesWithUnsetPayoffInstr() {

        notificationService.notifyUnsetPayoffInstr();

        verify(citizenDAOMock, only()).findFiscalCodesWithUnsetPayoffInstr();
        verify(restConnector, times(3))
                .notify(Mockito.any(NotificationDTO.class));
    }

    @Test
    public void testUpdateRanking() throws IOException {

        notificationService.updateRanking();
        verify(citizenDAOMock, only()).updateRanking();
        verify(citizenDAOMock, times(1)).updateRanking();
    }

    @Test
    public void testUpdateWinners() throws IOException {

        notificationService.updateWinners();
        verify(citizenDAOMock, only()).updateWinners();
        verify(citizenDAOMock, times(1)).updateWinners();
    }

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

        BDDMockito.when(winnersService.sendWinners(Mockito.any(Long.class), Mockito.anyInt(), Mockito.any(), Mockito.any(LocalDateTime.class), Mockito.anyInt()))
                .thenAnswer(invocation -> 0);
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

        notificationService.sendWinners();

        verify(winnersService, atLeastOnce()).sendWinners(Mockito.any(Long.class), Mockito.anyInt(), Mockito.any(), Mockito.any(LocalDateTime.class), Mockito.anyInt());
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

        notificationService.sendWinners();

        verifyZeroInteractions(winnersService);
        verify(awardPeriodRestClientMock, only()).findAllAwardPeriods();
    }

}