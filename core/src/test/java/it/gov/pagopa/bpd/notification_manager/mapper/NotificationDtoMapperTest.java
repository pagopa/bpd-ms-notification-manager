package it.gov.pagopa.bpd.notification_manager.mapper;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NotificationDtoMapper.class)
public class NotificationDtoMapperTest {

    @Autowired
    private NotificationDtoMapper notificationDtoMapper;


    @Test
    public void testNotificationDtoMapper() {
        String FC = "fiscalCode";
        NotificationDTO notificationDTO = notificationDtoMapper.NotificationDtoMapper(FC);

        assertEquals(notificationDTO.getMessage().getFiscal_code(), FC);
    }
}