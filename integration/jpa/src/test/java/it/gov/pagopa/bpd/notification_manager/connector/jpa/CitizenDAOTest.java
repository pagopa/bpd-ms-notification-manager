package it.gov.pagopa.bpd.notification_manager.connector.jpa;

import eu.sia.meda.layers.connector.query.CriteriaQuery;
import it.gov.pagopa.bpd.common.connector.jpa.BaseCrudJpaDAOTest;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

public class CitizenDAOTest extends BaseCrudJpaDAOTest<CitizenDAO, WinningCitizen, Long> {

    @Data
    private static class CitizenCriteria implements CriteriaQuery<WinningCitizen> {
        private Long id;
    }

    @Autowired
    private CitizenDAO citizenDAO;


    @Override
    protected CriteriaQuery<? super WinningCitizen> getMatchAlreadySavedCriteria() {
        CitizenDAOTest.CitizenCriteria criteriaQuery = new CitizenDAOTest.CitizenCriteria();
        criteriaQuery.setId(getStoredId());

        return criteriaQuery;
    }

    @Override
    protected CitizenDAO getDao() {
        return citizenDAO;
    }


    @Override
    protected void setId(WinningCitizen entity, Long id) {
        entity.setId(id);
    }


    @Override
    protected Long getId(WinningCitizen entity) {
        return entity.getId();
    }


    @Override
    protected void alterEntityToUpdate(WinningCitizen entity) {
        entity.setPayoffInstr("changed");
    }


    @Override
    protected Function<Integer, Long> idBuilderFn() {
        return Long::valueOf;
    }


    @Override
    protected String getIdName() {
        return "id";
    }
}