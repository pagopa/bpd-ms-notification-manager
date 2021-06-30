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
    //    private final JdbcTemplate jdbcTemplate;
    private final String updateWinningCitizenStatusSql;
    private final String updateWinningCitizenStatusAndFilenameSql;
//    private final String findAllOrderedByTrxNumSql;
//    private final RowMapperResultSetExtractor<WinningJdbcCitizen> findAllResultSetExtractor = new RowMapperResultSetExtractor<>(new CitizenRankingMapper());


    @Autowired
    public CitizenJdbcDAOImpl(JdbcTemplate jdbcTemplate) {

//        this.jdbcTemplate = jdbcTemplate;
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        updateWinningCitizenStatusSql = "update bpd_award_winner set status_s = :status, " +
                " update_date_t = :updateDate, update_user_s = :updateUser where id_n = :id";
        updateWinningCitizenStatusAndFilenameSql = "update bpd_award_winner set status_s = :status, " +
                " chunk_filename_s = :chunkFilename, " +
                " update_date_t = :updateDate, update_user_s = :updateUser where id_n = :id";
//        findAllOrderedByTrxNumSql = "select * from bpd_award_winner where award_period_id_n = ?," +
//                " AND enabled_b = true " +
//                " AND payoff_instr_s IS NOT NULL " +
//                " AND AND baw.status_s <> 'SENT'";
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

//    @Override
//    public List<WinningJdbcCitizen> findWinners(Long endingPeriodId, Long maxRow) {
//        if (log.isTraceEnabled()) {
//            log.trace("CitizenJdbcDaoImpl.findWinners");
//        }
//        if (log.isDebugEnabled()) {
//            log.debug("awardPeriodId = {}", endingPeriodId);
//        }
//
//        StringBuilder clauses = new StringBuilder();
//        clauses.append(" LIMIT ").append(maxRow);
//
//
//        return jdbcTemplate.query(connection -> connection.prepareStatement(findAllOrderedByTrxNumSql + clauses),
//                preparedStatement -> preparedStatement.setLong(1, endingPeriodId),
//                findAllResultSetExtractor);
//    }
//
//
//    @Slf4j
//    static final class CitizenRankingMapper implements RowMapper<WinningJdbcCitizen> {
//
//        public WinningJdbcCitizen mapRow(ResultSet rs, int rowNum) throws SQLException {
//            return WinningJdbcCitizen.builder()
//                    .fiscalCode(rs.getString("fiscal_code_c"))
//                    .awardPeriodId(rs.getLong("award_period_id_n"))
//                    ///TODO completare
//                    .build();
//        }
//    }
}

