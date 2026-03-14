/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.externalid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import org.jboss.as.ejb3.timerservice.ExtendedTimerConfig;
import org.jboss.as.ejb3.timerservice.ExtendedTimerService;
import org.junit.Assert;

@Stateless
public class ExternalIdTimerBean {

    @Resource
    private TimerService timerService;

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource dataSource;

    public void createTimerWithExternalId(String externalId) {
        ExtendedTimerConfig config = new ExtendedTimerConfig(null, true, externalId);

        // Create a single-action timer scheduled 1 day in the future so it doesn't fire during the test
        timerService.createSingleActionTimer(1000 * 60 * 60 * 24, config);
    }

    /**
     * Verifies that the EJB timer internals correctly inserted the external ID
     * by querying the database directly.
     */
    public int getActiveTimersCountByExternalIdViaSql(String externalId) throws Exception {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM JBOSS_EJB_TIMER WHERE EXTERNAL_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        }
        return count;
    }

    /**
     * Verifies the API extension by casting the injected TimerService
     * to WildFlyTimerService and fetching the timers.
     */
    public int getActiveTimersCountByExternalIdViaApi(String externalId) throws Exception {
        // Cast the injected proxy to our WildFly-specific interface
        ExtendedTimerService wildFlyTimerService = (ExtendedTimerService) timerService;

        Collection<Timer> activeTimers = wildFlyTimerService.getTimersByExternalId(externalId);
        return activeTimers != null ? activeTimers.size() : 0;
    }

    /**
     * EJB timeout callback.
     * This method is not expected to be invoked during the execution of this test,
     * as the timers are intentionally scheduled far into the future.
     */
    @Timeout
    public void timeout() {
        Assert.fail("Unexpected timer execution: this timer should not have triggered during the test run.");
    }
}