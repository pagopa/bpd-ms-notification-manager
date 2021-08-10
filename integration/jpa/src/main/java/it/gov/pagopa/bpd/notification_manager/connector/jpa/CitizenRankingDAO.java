package it.gov.pagopa.bpd.notification_manager.connector.jpa;

import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.CitizenRanking;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.CitizenRankingId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


/**
 * Data Access Object to manage all CRUD operations to the database
 */
@Repository
public interface CitizenRankingDAO extends CrudJpaDAO<CitizenRanking, CitizenRankingId> {

    @Query(nativeQuery = true, value = "SELECT bc.fiscal_code_s AS fiscal_code_c, " +
            "bre.award_period_id_n, " +
            "bc.enabled_b, " +
            "CASE WHEN bcr.cashback_n>bre.period_cashback_max_n THEN bre.period_cashback_max_n ELSE bcr.cashback_n END AS cashback_n, " +
            "bcr.id_trx_pivot, " +
            "bcr.cashback_norm_pivot, " +
            "bcr.id_trx_min_transaction_number, " +
            "coalesce(bcr.transaction_n,0) as transaction_n, " +
            "coalesce(bcr.ranking_n, bre.total_participants +1) AS ranking_n " +
            "FROM bpd_citizen bc " +
            "CROSS JOIN bpd_ranking_ext bre " +
            "LEFT OUTER JOIN bpd_citizen_ranking bcr ON bc.fiscal_code_s = bcr.fiscal_code_c " +
            "AND bcr.award_period_id_n = :awardPeriodId " +
            "WHERE bc.enabled_b IS TRUE " +
            "AND (bc.notification_step_s IS null OR bc.notification_step_s NOT LIKE '%' || :step || '%') " +
            "AND bre.award_period_id_n = :awardPeriodId " +
            "AND bc.timestamp_tc_t < :endPeriodDate " +
            "ORDER BY bcr.transaction_n DESC, bcr.fiscal_code_c " +
            "LIMIT :limit")
    List<CitizenRanking> extractRankingByAwardPeriodOrderByTransactionFiscalCode(
            @Param("awardPeriodId") Long awardPeriodId,
            @Param("step") String step,
            @Param("limit") Long limit,
            @Param("endPeriodDate") LocalDate endPeriodDate);
}