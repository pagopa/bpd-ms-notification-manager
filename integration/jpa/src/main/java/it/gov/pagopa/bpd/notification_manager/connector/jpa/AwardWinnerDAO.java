//package it.gov.pagopa.bpd.notification_manager.connector.jpa;
//
//import it.gov.pagopa.bpd.common.connector.jpa.CrudJpaDAO;
//import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
//import org.springframework.data.jpa.repository.Query;
//
//import java.util.List;
//
//public interface AwardWinnerDAO extends CrudJpaDAO<WinningCitizen, Long> {
//
//    @Query("SELECT a FROM WinningCitizen a WHERE a.awardPeriodId = 2")
//    List<WinningCitizen> findWinners();
//}
