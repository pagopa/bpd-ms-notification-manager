package it.gov.pagopa.bpd.notification_manager.connector.jdbc;


import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningJdbcCitizen;

import java.util.List;

public interface CitizenJdbcDAO {

    int[] updateWinningCitizen(List<WinningJdbcCitizen> winningJdbcCitizens);

    List<WinningJdbcCitizen> findWinners(Long endingPeriodId, Long maxRow);
}
