//package it.gov.pagopa.bpd.notification_manager.connector.jpa;
//
//import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Service;
//
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import javax.transaction.Transactional;
//import java.util.List;
//
//@Service
//public class CitizenDAOImpl implements CitizenDAO {
//
//    public static final String QUERY_CHECK_IBAN = "SELECT fiscal_code_s FROM bpd_citizen WHERE payoff_instr_s IS NULL";
//    public static final String QUERY_UPDATE_RANKING = "SELECT * from update_bpd_citizen_ranking()";
//    @PersistenceContext
//    private EntityManager em;
//
//    @Override
//    public List<String> findFiscalCodesWithUnsetPayoffInstr() {
//        //noinspection unchecked
//        return em.createNativeQuery(QUERY_CHECK_IBAN).getResultList();
//    }
//
//    @Override
//    @Transactional
//    public int updateRankingAndWinners() {
//        //noinspection unchecked
//        return em.createNativeQuery(QUERY_UPDATE_RANKING, WinningCitizen.class).getFirstResult();
//    }
//
//
//}
