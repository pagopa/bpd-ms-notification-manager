package it.gov.pagopa.bpd.notification_manager.dao.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
public class WinningCitizen {

    @Id
    private String fiscal_code_out;
    private String payoff_instr_out;
    private Long aw_period_out;
    private Long ranking_out;
    private LocalDate aw_period_start_out;
    private LocalDate aw_period_end_out;
}
