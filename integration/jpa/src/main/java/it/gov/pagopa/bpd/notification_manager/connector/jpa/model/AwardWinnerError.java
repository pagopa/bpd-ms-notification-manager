package it.gov.pagopa.bpd.notification_manager.connector.jpa.model;

import it.gov.pagopa.bpd.common.connector.jpa.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(of = {"idError"}, callSuper = false)
@Entity
@Data
@Table(name = "bpd_award_winner_error_notify")
public class AwardWinnerError extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpd_award_bpd_ward_error_id_seq")
    @SequenceGenerator(name="bpd_award_bpd_ward_error_id_seq", sequenceName = "bpd_award_bpd_ward_error_id_seq", allocationSize = 1)
    @Column(name = "id_error_n")
    private Long idError;

    @Column(name = "id_n")
    private Long id;

    @Column(name = "fiscal_code_s")
    private String fiscalCode;

    @Column(name = "award_period_id_n")
    private Long awardPeriodId;

    @Column(name = "error_code_s")
    private String errorCode;

    @Column(name = "error_message_s")
    private String errorMessage;
}
