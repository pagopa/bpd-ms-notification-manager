package it.gov.pagopa.bpd.notification_manager.connector.jdbc.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Id;
import java.time.OffsetDateTime;

@Data
@Builder
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class WinningCitizenDto {

    @Id
    @Column(name = "id_n")
    private Long id;
    @Column(name = "chunk_filename_s")
    private String chunkFilename;
    @Column(name = "status_s")
    private String status;
    @Column(name = "update_date_t")
    private OffsetDateTime updateDate;
    @Column(name = "update_user_s")
    private String updateUser;
}
