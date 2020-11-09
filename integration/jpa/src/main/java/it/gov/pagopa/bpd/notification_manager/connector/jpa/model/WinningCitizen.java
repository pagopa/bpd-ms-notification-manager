package it.gov.pagopa.bpd.notification_manager.connector.jpa.model;

import it.gov.pagopa.bpd.common.connector.jpa.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
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

}
