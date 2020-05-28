package it.gov.pagopa.bpd.notification_manager.connector.jpa;

import it.gov.pagopa.bpd.common.connector.jpa.BaseJpaIntegrationTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

@ContextConfiguration(classes = CitizenDAOImpl.class)
public class NotificationManagerDAOTest extends BaseJpaIntegrationTest {

    @Autowired
    private CitizenDAOImpl citizenDAO;

    @Test
    public void test() {
        final List<String> result = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.size());
    }

}