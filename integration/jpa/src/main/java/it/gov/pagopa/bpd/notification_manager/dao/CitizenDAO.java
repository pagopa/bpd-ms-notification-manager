package it.gov.pagopa.bpd.notification_manager.dao;


import it.gov.pagopa.bpd.notification_manager.dao.model.WinningCitizen;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDAO {

    List<String> findFiscalCodesWithUnsetPayoffInstr();

    List<WinningCitizen> updateRankingAndFindWinners();

}
