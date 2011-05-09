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

import java.util.Collections;

import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests of session expiration
 *
 * @author Brian Stansberry
 */
public class ReplicationToPassivatedSessionUnitTestCase extends InfinispanCacheContainerTest {
    protected static long testId = System.currentTimeMillis();

    protected Logger log = Logger.getLogger(getClass());

    protected DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    protected ReplicationToPassivatedSessionUnitTestCase(int containers, boolean local, boolean passivate,
            Boolean totalReplication) {
        super(containers, local, passivate, totalReplication);
    }

    public ReplicationToPassivatedSessionUnitTestCase() {
        super(2, false, true, true);
    }

    @After
    @Override
    public void stop() throws Exception {
        for (DistributableSessionManager<?> manager : managers) {
            if (manager != null) {
                try {
                    manager.stop();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
        super.stop();
    }

    protected ReplicationGranularity getReplicationGranularity() {
        return ReplicationGranularity.SESSION;
    }

    protected ReplicationTrigger getReplicationTrigger() {
        return ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET;
    }

    @Test
    public void testReplicationToPassivatedSession() throws Exception {
        log.info("++++ Starting testReplicationToPassivatedSession ++++");

        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 30 mins and a maxIdle of 1
        this.startManagers(warname, 1800000, 1, -1);

        Object value = "0";
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value),
                false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        String id = setHandler.getSessionId();

        SessionTestUtil.sleepThread(1100);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        value = "1";
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, id);

        assertEquals("0", setHandler.getCheckedAttributes().get("count"));

        value = "2";
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
        SessionTestUtil.invokeRequest(managers[1], setHandler, id);

        assertEquals("1", setHandler.getCheckedAttributes().get("count"));
    }

    @Test
    public void testFailoverToPassivatedSession() throws Exception {
        log.info("++++ Starting testFailoverToPassivatedSession ++++");

        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 30 mins and a maxIdle of 1
        this.startManagers(warname, 1800000, 1, -1);

        Object value = "0";
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        String id = setHandler.getSessionId();

        SessionTestUtil.sleepThread(1100);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        value = "1";
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", value), false);
        SessionTestUtil.invokeRequest(managers[1], setHandler, id);

        assertEquals("0", setHandler.getCheckedAttributes().get("count"));
    }

    protected void startManagers(String warname, int maxInactive, int maxIdle, int maxUnreplicated) throws Exception {
        for (int i = 0; i < cacheContainers.length; ++i) {
            JBossWebMetaData metadata = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), -1, true, maxIdle, -1, true, (i == 0) ? maxUnreplicated : -1);
            managers[i] = SessionTestUtil.createManager(metadata, warname, maxInactive, cacheContainers[i], null);
            managers[i].start();
        }
    }

}
