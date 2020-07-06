package it.gov.pagopa.bpd.notification_manager.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class ContentPrescriptionData {

    @NotNull
    @NotBlank
    @Length(min = 15, max = 15)
    private String nre;

    @Length(min = 1, max = 16)
    private String iub;

    @JsonProperty("prescriber_fiscal_code")
    private String prescriberFiscalCode;
}

