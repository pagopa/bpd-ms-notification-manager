package it.gov.pagopa.bpd.notification_manager.connector.jdbc;


import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningCitizenDto;

import java.util.List;

public interface CitizenJdbcDAO {

    int[] updateWinningCitizenStatus(List<WinningCitizenDto> winningCitizenDtos);

    int[] updateWinningCitizenStatusAndFilename(List<WinningCitizenDto> winningCitizenDtos);

//    List<WinningCitizenDto> findWinners(Long endingPeriodId, Long maxRow);

//    int[] updateWinningCitizenStatus(List<WinningCitizenDto> winningJdbcCitizens, WinningCitizenDto.Status status);
}
