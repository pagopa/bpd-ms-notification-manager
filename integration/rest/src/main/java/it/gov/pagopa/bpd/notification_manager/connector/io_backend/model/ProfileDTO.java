package it.gov.pagopa.bpd.notification_manager.connector.io_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class ProfileDTO {

    @JsonProperty("fiscal_code")
    @NotNull
    @NotBlank
    private String fiscalCode;
}
