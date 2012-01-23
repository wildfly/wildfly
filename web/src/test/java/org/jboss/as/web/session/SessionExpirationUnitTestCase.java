/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.apache.catalina.Session;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of session expiration
 *
 * TODO move some of the other expiration tests here where we can more closely control the test timing
 *
 * @author Brian Stansberry
 */
public class SessionExpirationUnitTestCase {
    private static final Logger log = Logger.getLogger(SessionExpirationUnitTestCase.class);

    private static long testCount = System.currentTimeMillis();

    private JGroupsSystemPropertySupport jgroupsSupport;
    private EmbeddedCacheManager[] cacheContainers = new EmbeddedCacheManager[2];
    private DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    @Before
    public void setUp() throws Exception {
        // Set system properties to properly bind JGroups channels
        jgroupsSupport = new JGroupsSystemPropertySupport();
        jgroupsSupport.setUpProperties();
    }

    @After
    public void tearDown() throws Exception {
        // Restore any system properties we set in setUp
        if (jgroupsSupport != null) {
            jgroupsSupport.restoreProperties();
        }

        for (DistributableSessionManager<?> manager : managers) {
            if (manager != null) {
                try {
                    manager.stop();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }

        for (CacheContainer cacheContainer : cacheContainers) {
            if (cacheContainer != null) {
                try {
                    cacheContainer.stop();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Test for JBAS-5404
     *
     * @throws Exception
     */
    @Test
    public void testMaxInactiveIntervalReplication() throws Exception {
        log.info("Enter testMaxInactiveIntervalReplication");

        ++testCount;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(2);
        for (int i = 0; i < cacheContainers.length; ++i) {
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, null, true, false);
            cacheContainers[i].start();

            managers[i] = SessionTestUtil.createManager(webMetaData, "test" + testCount, 5, cacheContainers[i], null);
            managers[i].start();
        }

        // Set up a session
        String id = "1";
        Session sess = managers[0].findSession(id);
        assertNull("session does not exist", sess);

        sess = managers[0].createSession(id, new Random());
        sess.access();
        sess.getSession().setAttribute("test", "test");
        managers[0].storeSession(sess);
        sess.endAccess();

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 1, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());

        // Confirm a session timeout clears space
        sess = managers[0].findSession(id);
        sess.access();
        sess.setMaxInactiveInterval(1);
        managers[0].storeSession(sess);
        sess.endAccess();

        SessionTestUtil.sleepThread(1005);

        managers[1].backgroundProcess();

        assertEquals("Session count correct", 1, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 1, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 0, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());

        managers[0].backgroundProcess();

        assertEquals("Session count correct", 0, managers[0].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[0].getLocalActiveSessionCount());
        assertEquals("Session count correct", 0, managers[1].getActiveSessionCount());
        assertEquals("Local session count correct", 0, managers[1].getLocalActiveSessionCount());
    }
}
