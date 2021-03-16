package it.gov.pagopa.bpd.notification_manager.connector.jpa.model;

import it.gov.pagopa.bpd.common.connector.jpa.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Entity
@Data
@Table(name = "bpd_award_winner")
public class WinningCitizen extends BaseEntity {

    @Id
    @Column(name = "id_n")
    private Long id;

    @Column(name = "cashback_n")
    private BigDecimal cashback;

    @Column(name = "fiscal_code_s")
    private String fiscalCode;

    @Column(name = "account_holder_cf_s")
    private String accountHolderFiscalCode;

    @Column(name = "payoff_instr_s")
    private String payoffInstr;

    @Column(name = "account_holder_name_s")
    private String accountHolderName;

    @Column(name = "account_holder_surname_s")
    private String accountHolderSurname;

    @Column(name = "check_instr_status_s")
    private String checkInstrStatus;

    @Column(name = "award_period_id_n")
    private Long awardPeriodId;

    @Column(name = "aw_period_start_d")
    private LocalDate awardPeriodStart;

    @Column(name = "aw_period_end_d")
    private LocalDate awardPeriodEnd;

    @Column(name = "amount_n")
    private BigDecimal amount;

    @Column(name = "typology_s")
    private String typology;

    @Column(name = "jackpot_n")
    private BigDecimal jackpot;

    @Column(name = "technical_account_holder_s")
    private String technicalAccountHolder;

    @Column(name = "chunk_filename_s")
    private String chunkFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_s")
    private Status status;

    @Column(name = "esito_bonifico_s")
    private String esitoBonifico;

    @Column(name = "cro_s")
    private String cro;

    @Column(name = "data_esecuzione_t")
    private LocalDate bankTransferDate;

    @Column(name = "result_reason_s")
    private String resultReason;

    @Column(name = "to_notify_b")
    private Boolean toNotify;

    @Column(name = "notify_times_n")
    private Long notifyTimes;

    @Column(name = "notify_id_s")
    private String notifyId;

    @Column(name = "ticket_id_n")
    private Long ticketId;

    @Column(name = "related_id_n")
    private Long relatedUniqueId;

    @Column(name = "consap_id_n")
    Long consapId;

    public enum Status {
        NEW, SENT, RECOVERY, INTEGRATION
    }
}
