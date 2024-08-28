/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.MonitoringObjectId;
import com.ericsson.oss.apps.model.StateEnum;

@Repository
public interface MonitoringObjectRepository extends JpaRepository<MonitoringObject, MonitoringObjectId> {

    long deleteByPmeSessionId(String pmeSessionId);

    List<MonitoringObject> findAllByStateAndPmeSessionIdIn(StateEnum state, List<String> pmeSessionId);

    List<MonitoringObject> findAllByPmeSessionId(String pmeSessionId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MonitoringObject mo "
            + "SET mo.state = 'STOPPED' "
            + "WHERE mo.pmeSessionId = :pmeSessionId and mo.state = 'ENABLED' and mo.lastProcessedTime = mo.endTime")
    int updateMonitoringObjectStateByPmeSessionId(@Param("pmeSessionId") String pmeSessionId);

    @Query("SELECT COUNT(mo) > 0 FROM MonitoringObject mo "
            + "WHERE mo.state = 'ENABLED' AND mo.pmeSessionId = :pmeSessionId")
    boolean existsEnabledMonitoringObjectsByPmeSessionId(@Param("pmeSessionId") String pmeSessionId);
}