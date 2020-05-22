package it.gov.pagopa.bpd.notification_manager.dao.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
public class WinningCitizen {

    @Id
    @Column(name = "fiscal_code_out")
    private String fiscalCode;

    @Column(name = "payoff_instr_out")
    private String payoffInstr;

    @Column(name = "aw_period_out")
    private Long awardPeriodId;

    @Column(name = "ranking_out")
    private Long ranking;

    @Column(name = "amount_out")
    private Long amount;

    @Column(name = "aw_period_start_out")
    private LocalDate awardPeriodStart;

    @Column(name = "aw_period_end_out")
    private LocalDate awardPeriodEnd;
}
