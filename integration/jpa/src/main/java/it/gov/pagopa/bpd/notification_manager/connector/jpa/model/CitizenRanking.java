package it.gov.pagopa.bpd.notification_manager.connector.jpa.model;

import it.gov.pagopa.bpd.common.connector.jpa.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Data
@IdClass(CitizenRankingId.class)
@NoArgsConstructor
@EqualsAndHashCode(of = {"fiscalCode", "awardPeriodId"}, callSuper = false)
@Table(name = "bpd_citizen_ranking")
public class CitizenRanking implements Serializable {


    @Id
    @Column(name = "fiscal_code_c")
    private String fiscalCode;

    @Id
    @Column(name = "award_period_id_n")
    private Long awardPeriodId;

    @Column(name = "cashback_n")
    private BigDecimal totalCashback;

    @Column(name = "transaction_n")
    private Long transactionNumber;

    @Column(name = "ranking_n")
    private Long ranking;

    @Column(name = "id_trx_pivot")
    private String idTrxPivot;

    @Column(name = "cashback_norm_pivot")
    private BigDecimal cashbackNormPivot;

    @Column(name = "id_trx_min_transaction_number")
    private String idTrxMinTransactionNumber;

}
