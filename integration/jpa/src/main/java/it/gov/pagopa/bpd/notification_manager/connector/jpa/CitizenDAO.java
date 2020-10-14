package it.gov.pagopa.bpd.notification_manager.connector.jpa;


import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDAO {

    List<String> findFiscalCodesWithUnsetPayoffInstr();

    List<WinningCitizen> updateRankingAndFindWinners();

}
