package it.gov.pagopa.bpd.notification_manager.connector.jdbc;

import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningJdbcCitizen;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
class CitizenJdbcDAOImpl implements CitizenJdbcDAO {


    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final String updateWinningCitizenSql;
    private final String findAllOrderedByTrxNumSql;
    private final RowMapperResultSetExtractor<WinningJdbcCitizen> findAllResultSetExtractor = new RowMapperResultSetExtractor<>(new CitizenRankingMapper());


    @Autowired
    public CitizenJdbcDAOImpl(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        updateWinningCitizenSql = "update bpd_award_winner set status_s = :status, " +
                " chunk_filename_s = :chunkFilename, " +
                " update_date_t = :updateDate, update_user_s = :updateUser where id_n = :id";
        findAllOrderedByTrxNumSql = "select * from bpd_award_winner where award_period_id_n = ?," +
                " AND enabled_b = true " +
                " AND payoff_instr_s IS NOT NULL " +
                " AND AND baw.status_s <> 'SENT'";
    }

    @Override
    public int[] updateWinningCitizen(final List<WinningJdbcCitizen> winningJdbcCitizens) {
        if (log.isTraceEnabled()) {
            log.trace("CitizenJdbcDAOImpl.updateProcessedTransaction");
        }
        if (log.isDebugEnabled()) {
            log.debug("winningCitizenId = {}", winningJdbcCitizens);
        }

        SqlParameterSource[] batchValues = SqlParameterSourceUtils.createBatch(winningJdbcCitizens);
        return namedParameterJdbcTemplate.batchUpdate(updateWinningCitizenSql, batchValues);
    }

    @Override
    public List<WinningJdbcCitizen> findWinners(Long endingPeriodId, Long maxRow) {
        if (log.isTraceEnabled()) {
            log.trace("CitizenJdbcDaoImpl.findWinners");
        }
        if (log.isDebugEnabled()) {
            log.debug("awardPeriodId = {}", endingPeriodId);
        }

        StringBuilder clauses = new StringBuilder();
        clauses.append(" LIMIT ").append(maxRow);


        return jdbcTemplate.query(connection -> connection.prepareStatement(findAllOrderedByTrxNumSql + clauses),
                preparedStatement -> preparedStatement.setLong(1, endingPeriodId),
                findAllResultSetExtractor);
    }


    @Slf4j
    static final class CitizenRankingMapper implements RowMapper<WinningJdbcCitizen> {

        public WinningJdbcCitizen mapRow(ResultSet rs, int rowNum) throws SQLException {
            return WinningJdbcCitizen.builder()
                    .fiscalCode(rs.getString("fiscal_code_c"))
                    .awardPeriodId(rs.getLong("award_period_id_n"))
                    ///TODO completare
                    .build();
        }
    }
}

