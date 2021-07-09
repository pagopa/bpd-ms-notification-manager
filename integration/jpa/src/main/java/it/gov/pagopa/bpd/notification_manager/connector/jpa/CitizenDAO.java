package it.gov.pagopa.bpd.notification_manager.connector.jpa;


import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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
                    " AND baw.status_s not in ('SENT', 'WIP')")
    int countFindWinners(@Param("awardPeriodId") Long awardPeriodId);


    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s not in ('SENT', 'WIP')" +
                    " LIMIT :limit")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId,
                                     @Param("limit") Long limit);


    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.enabled_b = true" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.status_s = 'NEW'" +
                    " ORDER BY id_n" +
                    " LIMIT :limit" +
                    " OFFSET :offset")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId,
                                     @Param("limit") Long limit,
                                     @Param("offset") Long offset);


    @Modifying
    @Query("update WinningCitizen wc" +
            " set wc.status = :status," +
            " wc.chunkFilename = :chunkFilename," +
            " wc.updateDate = CURRENT_TIMESTAMP," +
            " wc.updateUser = 'RESTORE_WIP_WINNERS'" +
            " where wc.awardPeriodId = :awardPeriodId" +
            " and wc.status = 'WIP'")
    void restoreWinners(@Param("awardPeriodId") Long awardPeriodId,
                        @Param("status") WinningCitizen.Status status,
                        @Param("chunkFilename") String chunkFilename);


    @Modifying
    @Query("update WinningCitizen wc" +
            " set wc.status = :status," +
            " wc.updateDate = CURRENT_TIMESTAMP," +
            " wc.updateUser = 'RESTORE_WIP_WINNERS'" +
            " where wc.awardPeriodId = :awardPeriodId" +
            " and wc.status = 'WIP'")
    void restoreWinners(@Param("awardPeriodId") Long awardPeriodId,
                        @Param("status") WinningCitizen.Status status);


    @Query(nativeQuery = true,
            value = "SELECT CASE WHEN count(baw) > 0 THEN true ELSE false END" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.award_period_id_n = :awardPeriodId" +
                    " AND baw.status_s = 'WIP'" +
                    " LIMIT 1")
    boolean existsWorkingWinner(@Param("awardPeriodId") Long awardPeriodId);


    @Query(nativeQuery = true,
            value = "SELECT *" +
                    " FROM bpd_award_winner baw" +
                    " WHERE baw.enabled_b is true" +
                    " AND (:awardPeriodId = -1 OR baw.award_period_id_n = :awardPeriodId)" +
                    " AND baw.payoff_instr_s IS NOT NULL" +
                    " AND baw.amount_n >= 0.01" +
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

    @Query(nativeQuery = true, value = "SELECT * from update_ranking_with_milestone(:offset, :limit, :timestamp)")
    Integer updateRankingMilestone(@Param("offset") Integer offset,
                                   @Param("limit") Integer limit,
                                   @Param("timestamp") OffsetDateTime timestamp);

    @Query(nativeQuery = true, value = "SELECT * from update_bonifica_recesso_citizen(:citizenRange)")
    Boolean updateBonificaRecessoMonolitica(@Param("citizenRange") String citizenRange);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE bpd_citizen " +
            "SET notification_step_s= coalesce(notification_step_s,'') || :notStep || ',' " +
            "WHERE fiscal_code_s = :fiscalCode")
    void updateCitizenWithNotificationStep(@Param("fiscalCode") String fiscalCode,
                                           @Param("notStep") String notStep);

}
