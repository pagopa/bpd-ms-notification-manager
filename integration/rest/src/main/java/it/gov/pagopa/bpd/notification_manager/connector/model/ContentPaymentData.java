package it.gov.pagopa.bpd.notification_manager.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class ContentPaymentData {

    @NotNull
    @Min(value = 1)
    @Max(value = 9999999999L)
    private Long amount;

    @NotNull
    @NotBlank
    @JsonProperty("notice_number")
    private String noticeNumber;

    @JsonProperty("invalid_after_due_date")
    private Boolean invalidAfterDueDate = false;

}