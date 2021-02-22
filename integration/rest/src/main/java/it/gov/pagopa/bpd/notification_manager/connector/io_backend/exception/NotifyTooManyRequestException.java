package it.gov.pagopa.bpd.notification_manager.connector.io_backend.exception;

import eu.sia.meda.exceptions.MedaDomainRuntimeException;
import org.springframework.http.HttpStatus;

public class NotifyTooManyRequestException extends MedaDomainRuntimeException {
    public NotifyTooManyRequestException(HttpStatus responseStatus) {
        super("too many request", "generic.error", responseStatus);
    }
}
