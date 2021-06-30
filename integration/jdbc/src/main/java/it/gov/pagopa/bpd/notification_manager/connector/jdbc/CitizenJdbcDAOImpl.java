package it.gov.pagopa.bpd.notification_manager.connector.jdbc;

import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningCitizenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
class CitizenJdbcDAOImpl implements CitizenJdbcDAO {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final String updateWinningCitizenStatusSql;
    private final String updateWinningCitizenStatusAndFilenameSql;


    @Autowired
    public CitizenJdbcDAOImpl(JdbcTemplate jdbcTemplate) {
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        updateWinningCitizenStatusSql = "update bpd_award_winner set status_s = :status, " +
                " update_date_t = :updateDate, update_user_s = :updateUser where id_n = :id";
        updateWinningCitizenStatusAndFilenameSql = "update bpd_award_winner set status_s = :status, " +
                " chunk_filename_s = :chunkFilename, " +
                " update_date_t = :updateDate, update_user_s = :updateUser where id_n = :id";
    }


    @Override
    public int[] updateWinningCitizenStatus(List<WinningCitizenDto> winningCitizenDtos) {
        if (log.isDebugEnabled()) {
            log.trace("CitizenJdbcDAOImpl.updateWinningCitizenStatus");
            log.debug("winningCitizenDtos = {}", winningCitizenDtos);
        }

        SqlParameterSource[] batchValues = SqlParameterSourceUtils.createBatch(winningCitizenDtos);
        return namedParameterJdbcTemplate.batchUpdate(updateWinningCitizenStatusSql, batchValues);
    }


    @Override
    public int[] updateWinningCitizenStatusAndFilename(List<WinningCitizenDto> winningCitizenDtos) {
        if (log.isDebugEnabled()) {
            log.trace("CitizenJdbcDAOImpl.updateWinningCitizenStatusAndFilename");
            log.debug("winningCitizenDtos = {}", winningCitizenDtos);
        }

        SqlParameterSource[] batchValues = SqlParameterSourceUtils.createBatch(winningCitizenDtos);
        return namedParameterJdbcTemplate.batchUpdate(updateWinningCitizenStatusAndFilenameSql, batchValues);
    }

}

