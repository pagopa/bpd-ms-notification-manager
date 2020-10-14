package it.gov.pagopa.bpd.notification_manager.connector.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Data Transfer Object (input) for {@link it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient}
 */
@Data
public class NotificationDTO {

    @Min(value = 3600)
    @Max(value = 604800)
    @JsonProperty("time_to_live")
    private Long timeToLive;

    @JsonProperty("fiscal_code")
    @NotNull
    @NotBlank
    private String fiscalCode;

    @JsonProperty("default_addresses")
    private MessageAddresses defaultAddresses;

    @NotNull
    private MessageContent content;

}

