package it.gov.pagopa.bpd.notification_manager.connector.jpa;


import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDAO extends CrudJpaDAO<WinningCitizen, Long> {

    @Query(nativeQuery = true, value = "SELECT fiscal_code_s FROM bpd_citizen WHERE payoff_instr_s IS NULL")
    List<String> findFiscalCodesWithUnsetPayoffInstr();

    @Query(nativeQuery = true, value = "SELECT * from update_bpd_citizen_ranking()")
    Boolean updateRanking();

    @Query(nativeQuery = true, value = "SELECT 1 from update_bpd_award_winner()")
    void updateWinners();

    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s <> 'SENT'" +
                    " LIMIT :limit")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId,
                                     @Param("limit") Long limit);

    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s <> 'SENT'" +
                    " ORDER BY id_n" +
                    " OFFSET :offset" +
                    " LIMIT :limit")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId,
                                     @Param("offset") Long offset,
                                     @Param("limit") Long limit);

}
