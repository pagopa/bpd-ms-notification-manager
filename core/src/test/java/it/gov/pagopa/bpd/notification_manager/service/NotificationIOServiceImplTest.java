package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.AwardWinnerErrorDAO;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/notificationService.properties")
@ContextConfiguration(classes = {NotificationIOServiceImpl.class})
public class NotificationIOServiceImplTest {

    @MockBean
    private NotificationRestConnector restConnector;

    @MockBean
    private CitizenDAO citizenDAOMock;

    @MockBean
    private AwardWinnerErrorDAO awardWinnerErrorDAO;

    @SpyBean
    private NotificationDtoMapper notificationDtoMapper;

    @Autowired
    private NotificationIOServiceImpl notificationIOService;

    @PostConstruct
    public void configureMock() {
        BDDMockito.when(restConnector.notify(Mockito.any(NotificationDTO.class)))
                .thenAnswer(invocation -> {
                    NotificationResource result = new NotificationResource();
                    result.setId("ok");
                    return result;
                });

        BDDMockito.when(citizenDAOMock.findWinnersToNotify(Mockito.anyLong(),Mockito.anyLong(),Mockito.anyList(),Mockito.anyLong(),Mockito.anyLong()))
                .thenAnswer(invocation -> {
                    List<WinningCitizen> result = new ArrayList<>();
                    WinningCitizen citizen = new WinningCitizen();
                    citizen.setFiscalCode("allowed-fiscal-code");
                    citizen.setEsitoBonifico("OK");
                    citizen.setToNotify(Boolean.TRUE);
                    citizen.setNotifyTimes(0L);
                    citizen.setAmount(BigDecimal.valueOf(50));
                    citizen.setBankTransferDate(LocalDate.now());
                    citizen.setCro("cro");

                    result.add(citizen);
                    return result;
                });
    }

    @Test
    public void testNotifyWinners() throws IOException{
        notificationIOService.notifyWinnersPayments();

        verify(citizenDAOMock, times(1)).findWinnersToNotify(Mockito.anyLong(),Mockito.anyLong(),Mockito.anyList(),Mockito.anyLong(),Mockito.anyLong());
    }

}