package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.dao.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NotificationServiceImpl.class)
public class NotificationServiceImplTest {

    @MockBean
    private NotificationRestClient restClientMock;

    @MockBean
    private CitizenDAO citizenDAOMock;

    @SpyBean
    private NotificationDtoMapper notificationDtoMapper;

    @Autowired
    private NotificationServiceImpl notificationService;


    @PostConstruct
    public void configureMock() {
        BDDMockito.when(restClientMock.notify(Mockito.any(NotificationDTO.class), Mockito.anyString()))
                .thenAnswer(invocation -> {
                    NotificationResource result = new NotificationResource();
                    result.setMessage("ok");
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
    }

    @Test
    public void testFindFiscalCodesWithUnsetPayoffInstr() {

        notificationService.findFiscalCodesWithUnsetPayoffInstr();

        verify(citizenDAOMock, only()).findFiscalCodesWithUnsetPayoffInstr();
        verify(restClientMock, times(3))
                .notify(Mockito.any(NotificationDTO.class), Mockito.anyString());
    }

    @Test
    public void testUpdateRanking() {

        notificationService.updateRanking();
        verify(citizenDAOMock, only()).update_ranking();
        verify(citizenDAOMock, times(1)).update_ranking();
    }
}