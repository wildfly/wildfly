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

import static org.jboss.as.web.session.SessionTestUtil.blockUntilViewsReceived;
import static org.jboss.as.web.session.SessionTestUtil.createManager;
import static org.jboss.as.web.session.SessionTestUtil.createWebMetaData;
import static org.jboss.as.web.session.SessionTestUtil.getAttributeValue;
import static org.jboss.as.web.session.SessionTestUtil.invokeRequest;
import static org.jboss.as.web.session.SessionTestUtil.sleepThread;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.jboss.as.web.session.mocks.InvalidateSessionRequestHandler;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests of failover with buddy replication
 *
 * @author Brian Stansberry
 */
public class DistributionFailoverUnitTestCase extends InfinispanCacheContainerTest {
    protected static long testId = System.currentTimeMillis();

    protected Logger log = Logger.getLogger(getClass());

    protected DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    public DistributionFailoverUnitTestCase() {
        super(4, false, true, false);
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
    public void testInvalidateOnFailoverToBackup() throws Exception {
        this.log.info("++++ Starting testInvalidateOnFailoverToBackup ++++");

        String warname = String.valueOf(++testId);

        this.log.info("Starting managers");

        // A war with a maxInactive of 30 mins and a maxIdle of 1
        this.startManagers(warname, 1800000, 1, -1);

        this.log.info("Request(1) to manager[3]");

        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count",
                getAttributeValue(0)), false);
        invokeRequest(managers[3], setHandler, null);

        this.log.info("Request(2) to manager[3]");

        String id = setHandler.getSessionId();

        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(1)), false);
        invokeRequest(managers[3], setHandler, id);
        assertEquals(getAttributeValue(0), setHandler.getCheckedAttributes().get("count"));

        this.log.info("Sleeping");

        sleepThread(1100);

        this.log.info("Run passivation");

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();
        managers[2].backgroundProcess();
        managers[3].backgroundProcess();

        this.log.info("Request(3) to manager[3]");

        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(2)), false);
        invokeRequest(managers[3], setHandler, id);
        assertEquals(getAttributeValue(1), setHandler.getCheckedAttributes().get("count"));

        this.log.info("Request(4) to manager[3]");

        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(3)), false);
        invokeRequest(managers[3], setHandler, id);
        assertEquals(getAttributeValue(2), setHandler.getCheckedAttributes().get("count"));

        this.log.info("Invalidate request to manager[0]");

        // Invalidate on the failover request
        InvalidateSessionRequestHandler invalidationHandler = new InvalidateSessionRequestHandler(
                Collections.singleton("count"), false);
        invokeRequest(managers[0], invalidationHandler, id);
        assertEquals(getAttributeValue(3), invalidationHandler.getCheckedAttributes().get("count"));

        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(0)), false);
        invokeRequest(managers[0], invalidationHandler, id);
        assertNull(setHandler.getCheckedAttributes().get("count"));
    }

    @Test
    public void testFailoverAndFailBack() throws Throwable {
        log.info("++++ Starting testFailoverAndFailBack ++++");

        String warname = String.valueOf(++testId);
        // A war with a maxInactive of 30 mins and no maxIdle
        this.startManagers(warname, 1800000, -1, -1);

        log.info("managers created");
        log.info("creating session");
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count",
                getAttributeValue(0)), false);
        invokeRequest(managers[0], setHandler, null);

        String id = setHandler.getSessionId();

        // Find a node
        int localIndex = 0;
        int remoteIndex = 0;
        for (int i = 0; i < managers.length; ++i) {
            if (managers[i].getDistributedCacheManager().isLocal(id)) {
                localIndex = i;
            } else {
                remoteIndex = i;
            }
        }
        System.out.println(String.format("%s is local to node%d and non-local to node%d", id, localIndex, remoteIndex));
        // Modify
        log.info("modifying session");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(1)), false);
        invokeRequest(managers[localIndex], setHandler, id);
        assertEquals(getAttributeValue(0), setHandler.getCheckedAttributes().get("count"));

        // Failover and modify
        log.info("failing over");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(2)), false);
        invokeRequest(managers[localIndex], setHandler, id);
        assertEquals(getAttributeValue(1), setHandler.getCheckedAttributes().get("count"));

        // Modify
        log.info("modifying session");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(3)), false);
        invokeRequest(managers[localIndex], setHandler, id);
        assertEquals(getAttributeValue(2), setHandler.getCheckedAttributes().get("count"));

        // Failover and modify
        log.info("failing over");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(4)), false);
        invokeRequest(managers[remoteIndex], setHandler, id);
        assertEquals(getAttributeValue(3), setHandler.getCheckedAttributes().get("count"));

        // Modify
        log.info("modifying session");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(5)), false);
        invokeRequest(managers[remoteIndex], setHandler, id);
        assertEquals(getAttributeValue(4), setHandler.getCheckedAttributes().get("count"));

        // Failback and modify
        log.info("failing back");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(6)), false);
        invokeRequest(managers[localIndex], setHandler, id);
        assertEquals(getAttributeValue(5), setHandler.getCheckedAttributes().get("count"));

        // Invalidate
        log.info("invalidating");
        InvalidateSessionRequestHandler invalidationHandler = new InvalidateSessionRequestHandler(
                Collections.singleton("count"), false);
        invokeRequest(managers[localIndex], invalidationHandler, id);
        assertEquals(getAttributeValue(6), invalidationHandler.getCheckedAttributes().get("count"));

        // Reestablish
        log.info("re-establishing");
        setHandler = new SetAttributesRequestHandler(Collections.singletonMap("count", getAttributeValue(0)), false);
        invokeRequest(managers[localIndex], invalidationHandler, id);
        assertNull(setHandler.getCheckedAttributes().get("count"));
    }

    protected void startManagers(String warname, int maxInactive, int maxIdle, int maxUnreplicated) throws Exception {
        JBossWebMetaData metadata = createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), -1, true, maxIdle, -1, true, maxUnreplicated);
        for (int i = 0; i < cacheContainers.length; i++) {
            managers[i] = createManager(metadata, warname, maxInactive, cacheContainers[i], null);
            managers[i].start();
        }

        blockUntilViewsReceived(cacheContainers, 5000L);
    }
}
