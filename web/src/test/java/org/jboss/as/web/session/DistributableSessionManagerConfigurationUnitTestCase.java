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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.PassivationConfig;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests of session count management.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 108925 $
 */
public class DistributableSessionManagerConfigurationUnitTestCase {
    private static final Logger log = Logger.getLogger(DistributableSessionManagerConfigurationUnitTestCase.class);

    private static long testCount = System.currentTimeMillis();

    private EmbeddedCacheManager cacheContainer;
    private DistributableSessionManager<?> manager;
    private String tempDir;

    @After
    public void tearDown() throws Exception {
        if (manager != null) {
            try {
                manager.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        if (cacheContainer != null) {
            try {
                cacheContainer.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        if (tempDir != null) {
            SessionTestUtil.cleanFilesystem(tempDir);
        }
    }

    @Test
    public void testUseJK() throws Exception {
        log.info("Enter testUseJK");

        ++testCount;
        cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
        cacheContainer.start();

        JBossWebMetaData webMetaData = createWebMetaData(null, null, null, null, null);
        manager = SessionTestUtil.createManager(webMetaData, "test" + testCount, 5, cacheContainer, null);
        manager.start();

        assertTrue("With no config, using JK", manager.getUseJK());

        manager.stop();

        webMetaData = createWebMetaData(null, null, null, null, Boolean.TRUE);
        
        manager = SessionTestUtil.createManager(webMetaData, "test" + ++testCount, 5, cacheContainer, null);
        manager.start();

        assertTrue("With no jvmRoute but a config, using JK", manager.getUseJK());

        manager.stop();

        webMetaData = createWebMetaData(null, null, null, null, null);
        
        manager = SessionTestUtil.createManager(webMetaData, "test" + ++testCount, 5, cacheContainer, "test");
        manager.start();

        assertTrue("With jvmRoute set, using JK", manager.getUseJK());

        manager.stop();

        webMetaData = createWebMetaData(null, null, null, null, Boolean.FALSE);

        manager = SessionTestUtil.createManager(webMetaData, "test" + ++testCount, 5, cacheContainer, "test");
        manager.start();

        assertFalse("With a jvmRoute but config=false, not using JK", manager.getUseJK());
    }

    @Test
    public void testSnapshot() throws Exception {
        log.info("Enter testSnapshot");

        ++testCount;

        JBossWebMetaData webMetaData = createWebMetaData(null, null, null, null, null);

        cacheContainer = SessionTestUtil.createCacheContainer(true, null, false, false);
        cacheContainer.start();
        manager = SessionTestUtil.createManager(webMetaData, "test" + testCount, 5, cacheContainer, null);

        manager.start();

        assertEquals("With no config, using instant", SnapshotMode.INSTANT, manager.getSnapshotMode());

        manager.stop();

        webMetaData = createWebMetaData(null, null, null, null, Boolean.TRUE);
        webMetaData.getReplicationConfig().setSnapshotMode(SnapshotMode.INTERVAL);
        webMetaData.getReplicationConfig().setSnapshotInterval(new Integer(2));

        manager = SessionTestUtil.createManager(webMetaData, "test" + ++testCount, 5, cacheContainer, null);
        manager.start();

        assertEquals("With config, using interval", SnapshotMode.INTERVAL, manager.getSnapshotMode());
        assertEquals("With config, using 2 second interval", 2, manager.getSnapshotInterval());
    }

    private JBossWebMetaData createWebMetaData(Integer maxSessions, Boolean passivation, Integer maxIdle, Integer minIdle, Boolean useJK) {
        JBossWebMetaData webMetaData = new JBossWebMetaData();
        webMetaData.setDistributable(new EmptyMetaData());
        webMetaData.setMaxActiveSessions(maxSessions);
        PassivationConfig pcfg = new PassivationConfig();
        pcfg.setUseSessionPassivation(passivation);
        pcfg.setPassivationMaxIdleTime(maxIdle);
        pcfg.setPassivationMinIdleTime(minIdle);
        webMetaData.setPassivationConfig(pcfg);
        ReplicationConfig repCfg = new ReplicationConfig();
        repCfg.setReplicationGranularity(ReplicationGranularity.SESSION);
        repCfg.setReplicationTrigger(ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET);
        repCfg.setUseJK(useJK);
        webMetaData.setReplicationConfig(repCfg);
        return webMetaData;
    }
}
