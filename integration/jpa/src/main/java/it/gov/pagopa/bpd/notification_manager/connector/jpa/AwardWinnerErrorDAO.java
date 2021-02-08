package it.gov.pagopa.bpd.notification_manager.connector.jpa;


import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.AwardWinnerError;
import org.springframework.stereotype.Repository;

@Repository
public interface AwardWinnerErrorDAO extends CrudJpaDAO<AwardWinnerError, Long> {

}
