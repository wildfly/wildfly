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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for JBAS-7205. Deploy two wars on each node, sharing a cache between them. Confirm that redeploying one war results in
 * state being received.
 *
 * @author Brian Stansberry
 */
public class MultipleWarSingleRedeployTestCase extends InfinispanCacheContainerTest {
    protected static long testId = System.currentTimeMillis();

    protected Logger log = Logger.getLogger(getClass());

    protected DistributableSessionManager<?>[] managersA = new DistributableSessionManager[cacheContainers.length];
    protected DistributableSessionManager<?>[] managersB = new DistributableSessionManager[cacheContainers.length];

    protected Map<String, Object> allAttributes;

    public MultipleWarSingleRedeployTestCase() {
        super(2, false, false, null);
    }

    @Before
    @Override
    public void start() throws Exception {
        super.start();

        allAttributes = new HashMap<String, Object>();

        allAttributes.put("key", "value");

        allAttributes = Collections.unmodifiableMap(allAttributes);
    }

    @After
    @Override
    public void stop() throws Exception {
        for (DistributableSessionManager<?> manager : managersA) {
            if (manager != null) {
                try {
                    manager.stop();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }

        for (DistributableSessionManager<?> manager : managersB) {
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
    @org.junit.Ignore
    public void testMultipleWarSingleRedeploy() throws Exception {
        String warnameA = "A" + String.valueOf(++testId);
        this.startManagers(warnameA, managersA);

        String warnameB = "B" + String.valueOf(testId);
        this.startManagers(warnameB, managersB);

        // Establish session.
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managersA[0], setHandler, null);
        validateNewSession(setHandler);
        String idA = setHandler.getSessionId();

        setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managersB[0], setHandler, null);
        validateNewSession(setHandler);
        String idB = setHandler.getSessionId();

        BasicRequestHandler getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managersA[1], getHandler, idA);

        validateExpectedAttributes(allAttributes, getHandler);

        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managersB[1], getHandler, idB);

        validateExpectedAttributes(allAttributes, getHandler);

        // Undeploy one webapp on node 1
        managersB[1].stop();
        log.info("jbcmB1 stopped");

        // Deploy again
        managersB[1] = this.startManager(warnameB, cacheContainers[1]);

        log.info("jbcmB1 started");

        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managersA[1], getHandler, idA);

        validateExpectedAttributes(allAttributes, getHandler);

        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managersB[1], getHandler, idB);

        validateExpectedAttributes(allAttributes, getHandler);
    }

    protected void startManagers(String warname, DistributableSessionManager<?>[] managers) throws Exception {
        for (int i = 0; i < cacheContainers.length; i++) {
            managers[i] = this.startManager(warname, cacheContainers[i]);
        }
    }

    protected DistributableSessionManager<?> startManager(String warname, EmbeddedCacheManager cacheContainer) throws Exception {
        JBossWebMetaData metadata = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), true, 30);
        DistributableSessionManager<?> manager = SessionTestUtil.createManager(metadata, warname, 100, cacheContainer, null);
        manager.start();

        return manager;
    }

    protected void validateExpectedAttributes(Map<String, Object> expected, BasicRequestHandler handler) {
        assertFalse(handler.isNewSession());

        if (handler.isCheckAttributeNames()) {
            assertEquals(expected.size(), handler.getAttributeNames().size());
        }
        Map<String, Object> checked = handler.getCheckedAttributes();
        assertEquals(expected.size(), checked.size());
        for (Map.Entry<String, Object> entry : checked.entrySet()) {
            assertEquals(entry.getKey(), expected.get(entry.getKey()), entry.getValue());
        }

    }

    protected void validateNewSession(BasicRequestHandler handler) {
        assertTrue(handler.isNewSession());
        assertEquals(handler.getCreationTime(), handler.getLastAccessedTime());
        if (handler.isCheckAttributeNames()) {
            assertEquals(0, handler.getAttributeNames().size());
        }
        Map<String, Object> checked = handler.getCheckedAttributes();
        for (Map.Entry<String, Object> entry : checked.entrySet())
            assertNull(entry.getKey(), entry.getValue());
    }

}
