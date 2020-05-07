package it.gov.pagopa.bpd.notification_manager.connector;

import eu.sia.meda.BaseTest;
import eu.sia.meda.connector.rest.model.RestConnectorRequest;
import eu.sia.meda.connector.rest.model.RestConnectorResponse;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NotificationRestClientImplTest extends BaseTest {

    //TODO DA COMPLETARE
    private final NotificationRestClientImpl restClient;
    private final NotificationRestConnector connector;
    private final NotificationRequestTransformer requestTransformer;
    private final NotificationResponseTransformer responseTransformer;

    private HttpStatus httpStatus;
    @Captor
    private ArgumentCaptor<HashMap<String, String>> requestArgsCaptor;
    @Captor
    private ArgumentCaptor<RestConnectorRequest<NotificationDTO>> connectorRequestCaptor;
    @Captor
    private ArgumentCaptor<RestConnectorResponse<NotificationResource>> connectorResponseCaptor;


    public NotificationRestClientImplTest() {

        connector = Mockito.mock(NotificationRestConnector.class);
        requestTransformer = Mockito.spy(new NotificationRequestTransformer());
        responseTransformer = Mockito.spy(new NotificationResponseTransformer());
        restClient = new NotificationRestClientImpl(connector, requestTransformer, responseTransformer);

        configureTest();
    }

    private void configureTest() {
        when(connector.call(any(NotificationDTO.class), any(NotificationRequestTransformer.class),
                any(NotificationResponseTransformer.class), any(HashMap.class)))
                .thenAnswer(invocation -> {
                    final NotificationDTO input = invocation.getArgument(0, NotificationDTO.class);
                    final RestConnectorRequest<NotificationDTO> connectorRequest;
                    connectorRequest = requestTransformer.transform(input);
//                    if (invocation.getArguments().length < 4) {
//                        connectorRequest = requestTransformer.transform(input);
//
//                    } else {
//                        //noinspection RedundantArrayCreation
//                        connectorRequest = requestTransformer.transform(input,
//                                new Object[]{invocation.getArgument(3)});
//                    }

                    NotificationResource resource = new NotificationResource();
                    resource.setMessage("OK");
                    ResponseEntity<NotificationResource> responseEntity = new ResponseEntity<>(resource, httpStatus);
                    RestConnectorResponse<NotificationResource> restResponse = new RestConnectorResponse<>();
                    restResponse.setResponse(responseEntity);

                    return responseTransformer.transform(restResponse);
                });
    }

    @Test
    public void notify_OK() {
        httpStatus = HttpStatus.OK;

        NotificationDTO input = new NotificationDTO();
        input.getMessage().getContent().setDue_date("2018-10-13T00:00:00.000Z");
        input.getMessage().getContent().setMarkdown("Markdown");
        input.getMessage().getContent().setSubject("Subject");
        input.getMessage().setSender_service_id("senderServiceId");
        input.getMessage().setId("id");
        input.getMessage().setCreated_at("2018-10-13T00:00:00.000Z");
        input.getMessage().setFiscal_code("fiscalCode");
        input.getSender_metadata().setDepartment_name("departmentName");
        input.getSender_metadata().setService_name("serviceName");
        input.getSender_metadata().setOrganization_name("organizationName");

        final NotificationResource result = restClient.notify(input);

        assertNotNull(result);
        assertEquals("OK", result.getMessage());

//        checkInvocations(hashPan, input);

    }


}