/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence.legacy;

import java.io.File;
import java.io.InputStream;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the old non-xml class based serialization format can still be read
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(LegacyTimerFormatTestCase.LegacyTimerFormatTestCaseSetup.class)
public class LegacyTimerFormatTestCase {

    private static final String TIMER_LOCATION = "standalone/data/timer-service-data/leagacy-timer-persistence.leagacy-timer-persistence.LegacyTimerServiceBean/9ec70618-f985-420d-807a-364a2bcb1a81";

    public static class LegacyTimerFormatTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            String home = System.getProperty("jboss.home");
            File target = new File(home, TIMER_LOCATION);
            InputStream in = LegacyTimerFormatTestCase.class.getResourceAsStream("legacy-timer-data");
            try {
                FileUtils.copyFile(in, target);
            } finally {
                in.close();
            }

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        }
    }

    /**
     * must match between the two tests.
     */
    public static final String ARCHIVE_NAME = "leagacy-timer-persistence.war";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(LegacyTimerFormatTestCase.class.getPackage());
        return war;

    }

    @Test
    public void createTimerService() throws NamingException {
        Assert.assertTrue(LegacyTimerServiceBean.awaitTimerCall());
    }

}
