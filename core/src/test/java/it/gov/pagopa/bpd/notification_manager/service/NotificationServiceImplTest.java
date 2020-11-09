//package it.gov.pagopa.bpd.notification_manager.service;
//
//import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
//import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
//import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
//import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
//import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
//import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.BDDMockito;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.PostConstruct;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import static org.mockito.Mockito.*;
//
//@RunWith(SpringRunner.class)
//@TestPropertySource(properties = {
//        "core.NotificationService.notifyUnsetPayoffInstr.ttl=3600"
//})
//@ContextConfiguration(classes = NotificationServiceImpl.class)
//public class NotificationServiceImplTest {
//
//
//    @MockBean
//    private NotificationRestConnector restConnector;
//
//    @MockBean
//    private CitizenDAO citizenDAOMock;
//
//    @SpyBean
//    private NotificationDtoMapper notificationDtoMapper;
//
//    @Autowired
//    private NotificationServiceImpl notificationService;
//
//
//    @PostConstruct
//    public void configureMock() {
//        BDDMockito.when(restConnector.notify(Mockito.any(NotificationDTO.class)))
//                .thenAnswer(invocation -> {
//                    NotificationResource result = new NotificationResource();
//                    result.setId("ok");
//                    return result;
//                });
//        BDDMockito.when(citizenDAOMock.findFiscalCodesWithUnsetPayoffInstr())
//                .thenAnswer(invocation -> {
//                    ArrayList<String> result = new ArrayList<>();
//                    result.add("CF1");
//                    result.add("CF2");
//                    result.add("CF3");
//                    return result;
//                });
//    }
//
//    @Test
//    public void testFindFiscalCodesWithUnsetPayoffInstr() {
//
//        notificationService.notifyUnsetPayoffInstr();
//
//        verify(citizenDAOMock, only()).findFiscalCodesWithUnsetPayoffInstr();
//        verify(restConnector, times(3))
//                .notify(Mockito.any(NotificationDTO.class));
//    }
//
//    @Test
//    public void testUpdateRanking() throws IOException {
//
//        notificationService.updateRankingAndFindWinners();
//        verify(citizenDAOMock, only()).updateRankingAndFindWinners();
//        verify(citizenDAOMock, times(1)).updateRankingAndFindWinners();
//    }
//}