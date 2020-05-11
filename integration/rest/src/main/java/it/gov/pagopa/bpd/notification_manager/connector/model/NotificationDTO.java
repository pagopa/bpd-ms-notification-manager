package it.gov.pagopa.bpd.notification_manager.connector.model;


import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class NotificationDTO {

    private NotificationMessage message;
    private SenderMetadata sender_metadata;

    @Data
    public static class NotificationMessage {
        @NotBlank
        private String id;
        @NotBlank
        private String fiscal_code;
        @NotBlank
        private String created_at;
        @NotNull
        private Content content;
        @NotBlank
        private String sender_service_id;
    }

    @Data
    public static class Content {
        @NotBlank
        private String subject;
        @NotBlank
        private String markdown;
    }

    @Data
    public static class SenderMetadata {
        @NotBlank
        private String service_name;
        @NotBlank
        private String organization_name;
        @NotBlank
        private String department_name;
    }
}

