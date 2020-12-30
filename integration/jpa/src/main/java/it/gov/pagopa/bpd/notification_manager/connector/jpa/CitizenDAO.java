package it.gov.pagopa.bpd.notification_manager.connector.jpa;


import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDAO extends CrudJpaDAO<WinningCitizen, Long>{

    @Query(nativeQuery = true, value = "SELECT fiscal_code_s FROM bpd_citizen WHERE payoff_instr_s IS NULL")
    List<String> findFiscalCodesWithUnsetPayoffInstr();

    @Query(nativeQuery = true, value = "SELECT * from update_bpd_citizen_ranking()")
    Boolean updateRankingAndWinners();

    @Query("SELECT a FROM WinningCitizen a WHERE a.awardPeriodId = :awardPeriodId and a.enabled = true")
    List<WinningCitizen> findWinners(@Param("awardPeriodId") Long awardPeriodId);

}
