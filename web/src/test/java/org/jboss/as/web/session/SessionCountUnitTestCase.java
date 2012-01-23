/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.web.session;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of session count management.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 109806 $
 */
public class SessionCountUnitTestCase {
    private static int managerIndex = 1;

    private JGroupsSystemPropertySupport jgroupsSupport;
    private EmbeddedCacheManager[] cacheContainers = new EmbeddedCacheManager[2];
    private DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];
    private String tempDir;

    @Before
    public void setUp() throws Exception {
        // Set system properties to properly bind JGroups channels
        jgroupsSupport = new JGroupsSystemPropertySupport();
        jgroupsSupport.setUpProperties();

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File root = new File(tmpDir, getClass().getSimpleName());
        tempDir = root.getAbsolutePath();
    }

    @After
    public void tearDown() throws Exception {
        try {
            try {
                // Restore any system properties we set in setUp
                if (jgroupsSupport != null) {
                    jgroupsSupport.restoreProperties();
                }
    
                for (DistributableSessionManager<?> manager : managers) {
                    if (manager != null) {
                        try {
                            manager.stop();
                        } catch (Throwable e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
    
                for (EmbeddedCacheManager cacheContainer : cacheContainers) {
                    if ((cacheContainer != null) && cacheContainer.getStatus().stopAllowed()) {
                        try {
                            cacheContainer.stop();
                        } catch (Throwable e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            } finally {
                if (tempDir != null) {
                    SessionTestUtil.cleanFilesystem(tempDir);
                }
                String[] files = new File(tempDir).list();
                if (files != null) {
                    System.out.println(java.util.Arrays.asList(files));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            throw new Exception(e);
        }
    }

    @Test
    public void testStandaloneMaxSessions() throws Exception {
        System.out.println("Enter testStandaloneMaxSessions");

        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2);

        cacheContainers[0] = SessionTestUtil.createCacheContainer(true, null, false, false);
        cacheContainers[0].start();

        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 5, cacheContainers[0], null);
        managers[0].start();

        assertFalse("Passivation is disabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());

        // Set up a session
        Session sess1 = createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());

        createAndUseSession(managers[0], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());

        // Should fail to create a 3rd
        createAndUseSession(managers[0], "3", false, false);

        // Confirm a session timeout clears space
        sess1.setMaxInactiveInterval(1);
        SessionTestUtil.sleepThread(1100);

        createAndUseSession(managers[0], "3", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
    }

    @Test
    public void testStandaloneMaxSessionsWithMaxIdle() throws Exception {
        System.out.println("Enter testStandaloneMaxSessionsWithMaxIdle");

        String warName = "test" + ++managerIndex;
        String passDir = getPassivationDir(managerIndex, 1);
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 1, -1);

        cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
        cacheContainers[0].start();
        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 5, cacheContainers[0], null);
        managers[0].start();

        assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 1, managers[0].getMaxActiveAllowed());
        assertEquals("Correct max idle time", 1, managers[0].getPassivationMaxIdleTime());
        assertEquals("Correct min idle time", -1, managers[0].getPassivationMinIdleTime());

//        this.dump("1", "2");
        // Set up a session
        Session sess1 = createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[0], "2", false, false);

        // Confirm a session timeout clears space
        sess1.setMaxInactiveInterval(1);
        SessionTestUtil.sleepThread(1100);

        createAndUseSession(managers[0], "2", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        // Sleep past maxIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        createAndUseSession(managers[0], "3", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
    }

    @Test
    public void testStandaloneMaxSessionsWithMinIdle() throws Exception {
        System.out.println("Enter testStandaloneMaxSessionsWithMinIdle");

        String warName = "test" + ++managerIndex;
        String passDir = getPassivationDir(managerIndex, 1);
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);

        cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
        cacheContainers[0].start();
        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 5, cacheContainers[0], null);
        managers[0].start();

        assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 1, managers[0].getMaxActiveAllowed());
        assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
        assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());

        // Set up a session
        Session sess1 = createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[0], "2", false, false);

        // Confirm a session timeout clears space
        sess1.setMaxInactiveInterval(1);
        SessionTestUtil.sleepThread(1100);

        createAndUseSession(managers[0], "2", true, false);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());

        // Sleep past minIdleTime
        SessionTestUtil.sleepThread(1100);

        // assertTrue("Session 2 still valid", sess2.isValid());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        createAndUseSession(managers[0], "3", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 1, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
    }

    @Test
    public void testReplicatedMaxSessions() throws Exception {
        System.out.println("Enter testReplicatedMaxSessions");

        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1);
        for (int i = 0; i < cacheContainers.length; ++i) {
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, null, false, false);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 1, cacheContainers[i], null);
            managers[i].start();

            assertFalse("Passivation is disabled", managers[i].isPassivationEnabled());
            assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
            assertEquals("Correct max inactive interval", 1, managers[i].getMaxInactiveInterval());
        }

        SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);

        // Set up a session
        Session session = createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[1], "2", false, false);

        // Confirm a session timeout clears space
        session.setMaxInactiveInterval(1);
        useSession(managers[0], "1");
        SessionTestUtil.sleepThread(managers[0].getMaxInactiveInterval() * 1000 + 100);

        createAndUseSession(managers[1], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());

        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
    }

    @Test
    public void testReplicatedMaxSessionsWithMaxIdle() throws Exception {
        System.out.println("Enter testReplicatedMaxSessionsWithMaxIdle");

        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 1, -1);
        for (int i = 0; i < cacheContainers.length; ++i) {
            String passDir = getPassivationDir(managerIndex, i + 1);
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, false, false);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 1, cacheContainers[i], null);
            managers[i].start();

            assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
            assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
            assertEquals("Correct max idle time", 1, managers[i].getPassivationMaxIdleTime());
            assertEquals("Correct min idle time", -1, managers[i].getPassivationMinIdleTime());
        }

        SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);

        // Set up a session
        createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
        assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[1], "2", false, false);

        // Sleep past maxIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());

        createAndUseSession(managers[1], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());
    }

    @Test
    public void testReplicatedMaxSessionsWithMinIdle() throws Exception {
        System.out.println("Enter testReplicatedMaxSessionsWithMinIdle");

        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);
        for (int i = 0; i < cacheContainers.length; ++i) {
            String passDir = getPassivationDir(managerIndex, i + 1);
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, false, false);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 1, cacheContainers[i], null);
            managers[i].start();

            assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
            assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
            assertEquals("Correct max idle time", 3, managers[i].getPassivationMaxIdleTime());
            assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
        }

        SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);

        // Set up a session
        createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[1], "2", false, false);

        // Sleep past maxIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());

        createAndUseSession(managers[1], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());

    }

    @Test
    public void testTotalReplication() throws Exception {
        System.out.println("Enter testTotalReplication");

        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(1, true, 3, 1);
        for (int i = 0; i < cacheContainers.length; ++i) {
            String passDir = getPassivationDir(managerIndex, i + 1);
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, true, false);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 1, cacheContainers[i], null);
            managers[i].start();

            assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
            assertEquals("Correct max active count", 1, managers[i].getMaxActiveAllowed());
            assertEquals("Correct max idle time", 3, managers[i].getPassivationMaxIdleTime());
            assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
        }
        
        SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);

        // Set up a session
        createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());

        // Should fail to create a 2nd
        createAndUseSession(managers[1], "2", false, false);

        // Sleep past maxIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());

        createAndUseSession(managers[1], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());

        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());

    }

    @Test
    public void testStandaloneRedeploy() throws Exception {
        System.out.println("Enter testStandaloneRedeploy");

        standaloneWarRedeployTest(false);
    }

    @Test
    public void testStandaloneRestart() throws Exception {
        System.out.println("Enter testStandaloneRedeploy");

        standaloneWarRedeployTest(true);
    }

    private void standaloneWarRedeployTest(boolean restartCache) throws Exception {
        String warName = "test" + ++managerIndex;
        String passDir = getPassivationDir(managerIndex, 1);
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2, true, 3, 1);

        cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
        cacheContainers[0].start();

        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 300, cacheContainers[0], null);
        managers[0].start();

        assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
        assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
        assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());

        // Set up a session
        createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());

        // And a 2nd
        createAndUseSession(managers[0], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 2, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());

        // Sleep past minIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());

        createAndUseSession(managers[0], "3", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 2, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 3, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());

        managers[0].stop();

        if (restartCache) {
            cacheContainers[0].stop();

            passDir = getPassivationDir(managerIndex, 1);
            cacheContainers[0] = SessionTestUtil.createCacheContainer(true, passDir, false, false);
        }

        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 5, cacheContainers[0], null);
        managers[0].start();

        assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
        assertEquals("Correct max idle time", 3, managers[0].getPassivationMaxIdleTime());
        assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());

        // Sleep past minIdleTime
        SessionTestUtil.sleepThread(1100);

        createAndUseSession(managers[0], "4", true, true);
    }

    @Test
    public void testReplicatedRedeploy() throws Exception {
        System.out.println("Enter testReplicatedRedeploy");

        replicatedWarRedeployTest(false, false, false, false);
    }

    @Test
    public void testReplicatedRedeployWarAndCache() throws Exception {
        System.out.println("Enter testReplicatedRedeployWarAndCache");

        replicatedWarRedeployTest(true, false, false, false);
    }

    @Test
    public void testReplicatedRestart() throws Exception {
        System.out.println("Enter testReplicatedRestart");

        replicatedWarRedeployTest(true, true, false, false);
    }

    @Test
    public void testReplicatedRestartWithPurge() throws Exception {
        System.out.println("Enter testReplicatedRestartWithPurge");

        replicatedWarRedeployTest(true, true, false, true);
    }

    private void replicatedWarRedeployTest(boolean restartCache, boolean fullRestart, boolean totalReplication, boolean purgeOnStartStop) throws Exception {
        String warName = "test" + ++managerIndex;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2, true, 30, 1);
        for (int i = 0; i < cacheContainers.length; ++i) {
            String passDir = getPassivationDir(managerIndex, i + 1);
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, passDir, totalReplication, purgeOnStartStop);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 300, cacheContainers[i], null);
            managers[i].start();

            assertTrue("Passivation is enabled", managers[i].isPassivationEnabled());
            assertEquals("Correct max active count", 2, managers[i].getMaxActiveAllowed());
            assertEquals("Correct max idle time", 30, managers[i].getPassivationMaxIdleTime());
            assertEquals("Correct min idle time", 1, managers[i].getPassivationMinIdleTime());
        }

        SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);

        // Set up a session
        createAndUseSession(managers[0], "1", true, true);

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());

        // Create a 2nd
        createAndUseSession(managers[1], "2", true, true);

        assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[1].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[1].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());

        // Sleep past minIdleTime
        SessionTestUtil.sleepThread(1100);

        assertEquals("Passivated session count correct", 0, managers[1].getPassivatedSessionCount());

        createAndUseSession(managers[1], "3", true, true);

        // jbcm has 3 active because receipt of repl doesn't trigger passivation
        assertEquals("Session count correct", 3, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 1, managers[0].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
        // jbcm1 only has 2 active since it passivated one when it created 3rd
        assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
        // Both active sessions are local, as the remote session is oldest so we
        // passivate it first
        assertEquals("Local session count correct", 2, managers[1].getLocalActiveSessionCount());
        assertEquals("Created session count correct", 2, managers[1].getCreatedSessionCount());
        assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
        assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());

        if (fullRestart) {
            managers[1].stop();
            cacheContainers[1].stop();
        }

        managers[0].stop();

        if (restartCache) {
            cacheContainers[0].stop();

            String passDir = getPassivationDir(managerIndex, 1);
            cacheContainers[0] = SessionTestUtil.createCacheContainer(false, passDir, totalReplication, purgeOnStartStop);
            cacheContainers[0].start();
        }

        managers[0] = SessionTestUtil.createManager(webMetaData, warName, 300, cacheContainers[0], null);
        managers[0].start();

        assertTrue("Passivation is enabled", managers[0].isPassivationEnabled());
        assertEquals("Correct max active count", 2, managers[0].getMaxActiveAllowed());
        assertEquals("Correct max idle time", 30, managers[0].getPassivationMaxIdleTime());
        assertEquals("Correct min idle time", 1, managers[0].getPassivationMinIdleTime());

        // Do we expect content?
        boolean expectContent = true;
        // First, see if we expect a purge on redeploy
        // boolean expectPurge = purgeOnStartStop && (!totalReplication ||
        // marshalling);
        boolean expectPurge = purgeOnStartStop && restartCache;
        // Even with a purge, if the other cache is available we may have state
        // transfer
        // on redeploy
        if (expectPurge) {
            expectContent = !fullRestart && !totalReplication;
        }

        if (expectContent) {
            assertEquals("Session count correct", 2, managers[0].getActiveSessionCount());
            assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
            assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
            assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
            assertEquals("Passivated session count correct", 1, managers[0].getPassivatedSessionCount());
        } else {
            assertEquals("Session count correct", 0, managers[0].getActiveSessionCount());
            assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
            assertEquals("Created session count correct", 0, managers[0].getCreatedSessionCount());
            assertEquals("Expired session count correct", 0, managers[0].getExpiredSessionCount());
            assertEquals("Passivated session count correct", 0, managers[0].getPassivatedSessionCount());
        }

        if (!fullRestart) {
            assertEquals("Session count correct", 2, managers[1].getActiveSessionCount());
            assertEquals("Local session count correct", 2, managers[1].getLocalActiveSessionCount());
            assertEquals("Created session count correct", 2, managers[1].getCreatedSessionCount());
            assertEquals("Expired session count correct", 0, managers[1].getExpiredSessionCount());
            assertEquals("Passivated session count correct", 1, managers[1].getPassivatedSessionCount());
        }

        // Sleep past minIdleTime
        SessionTestUtil.sleepThread(1100);

        createAndUseSession(managers[0], "4", true, true);
    }

    @Test
    public void testTotalReplicatedRedeploy() throws Exception {
        System.out.println("Enter testTotalReplicatedRedeploy");

        replicatedWarRedeployTest(false, false, true, false);
    }

    @Test
    public void testTotalReplicatedRedeployWarAndCache() throws Exception {
        System.out.println("Enter testTotalReplicatedRedeployWarAndCache");

        replicatedWarRedeployTest(true, false, true, false);
    }

    @Test
    public void testTotalReplicatedRestart() throws Exception {
        System.out.println("Enter testTotalReplicatedRestart");

        replicatedWarRedeployTest(true, true, true, false);
    }

    @Test
    public void testTotalReplicatedRestartWithPurge() throws Exception {
        System.out.println("Enter testTotalReplicatedRestartWithPurge");

        replicatedWarRedeployTest(true, true, true, true);
    }

    private Session createAndUseSession(DistributableSessionManager<?> manager, String id, boolean canCreate, boolean access)
            throws Exception {
        
        // Shift to Manager interface when we simulate Tomcat
        Manager mgr = manager;
        Session sess = mgr.findSession(id);
        assertNull("session does not exist", sess);
        try {
            sess = mgr.createSession(id, new Random());
            if (!canCreate)
                fail("Could not create session" + id);
        } catch (IllegalStateException ise) {
            if (canCreate) {
                ise.printStackTrace(System.err);
                fail("Could create session " + id);
            }
        }

        if (access) {
            sess.access();
            sess.getSession().setAttribute("test", "test");

            manager.storeSession(sess);

            sess.endAccess();
        }

        return sess;
    }

    private void useSession(DistributableSessionManager<?> manager, String id) throws Exception {
        // Shift to Manager interface when we simulate Tomcat
        Manager mgr = manager;
        Session sess = mgr.findSession(id);
        assertNotNull("session exists", sess);

        sess.access();
        sess.getSession().setAttribute("test", "test");

        manager.storeSession(sess);

        sess.endAccess();
    }

    private String getPassivationDir(long testCount, int cacheCount) {
        File dir = new File(tempDir);
        dir = new File(dir, String.valueOf(testCount));
        dir.mkdirs();
        dir.deleteOnExit();
        dir = new File(dir, String.valueOf(cacheCount));
        dir.mkdirs();
        dir.deleteOnExit();
        return dir.getAbsolutePath();
    }
}
