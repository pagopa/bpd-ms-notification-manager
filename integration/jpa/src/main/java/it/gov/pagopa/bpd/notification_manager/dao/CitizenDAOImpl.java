package it.gov.pagopa.bpd.notification_manager.dao;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Service
public class CitizenDAOImpl implements CitizenDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<String> findFiscalCodesWithUnsetPayoffInstr() {

        String query = "SELECT fiscal_code_s FROM bpd_citizen WHERE payoff_instr_s IS NULL";
        //noinspection unchecked
        return em.createNativeQuery(query).getResultList();
    }

    @Override
    @Transactional
    public void update_ranking() {
        String query = "SELECT bpd_test.update_bpd_citizen_ranking();";
        em.createNativeQuery(query).getSingleResult();
    }


}
