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

    @Query(nativeQuery = true, value = "SELECT 1 from update_bpd_award_winner(:awardPeriodId)")
    void updateWinners(@Param("awardPeriodId") Long awardPeriodId);

    @Query(nativeQuery = true,
            value = "SELECT count(1)" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s <> 'SENT'" +
                    " AND baw.status_s <> 'INTEGRATION'")
    int countFindWinners(@Param("awardPeriodId") Long awardPeriodId);

    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s <> 'SENT'" +
                    " AND baw.status_s <> 'INTEGRATION'" +
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
                    " AND baw.status_s <> 'INTEGRATION'" +
                    " ORDER BY id_n" +
                    " OFFSET :offset" +
                    " LIMIT :limit")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId,
                                     @Param("offset") Long offset,
                                     @Param("limit") Long limit);

    @Query(nativeQuery=true,
            value="SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.enabled_b is true" +
                    " AND (:awardPeriodId = -1 OR baw.award_period_id_n = :awardPeriodId)" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s not in ('NEW','INTEGRATION')" +
                    " AND baw.to_notify_b is true" +
                    " AND baw.esito_bonifico_s IN (:resultList)" +
                    " AND (:notifyTimesLimit = -1 OR baw.notify_times_n < :notifyTimesLimit)" +
                    " AND EXISTS( SELECT * FROM bpd_citizen bc WHERE bc.fiscal_code_s = baw.fiscal_code_s" +
                    " AND bc.enabled_b IS true )" +
                    " ORDER BY id_n" +
                    " OFFSET :offset" +
                    " LIMIT :limit" +
                    " FOR UPDATE SKIP LOCKED"
    )
    List<WinningCitizen> findWinnersToNotify(@Param("awardPeriodId") Long awardPeriodId,
                                             @Param("notifyTimesLimit") Long notifyTimesLimit,
                                     @Param("resultList") List<String> resultList,
                                     @Param("offset") Long offset,
                                     @Param("limit") Long limit);

}
