package it.gov.pagopa.bpd.notification_manager.dao;


import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDAO {

    List<String> findFiscalCodesWithUnsetPayoffInstr();

    void update_ranking();

}
