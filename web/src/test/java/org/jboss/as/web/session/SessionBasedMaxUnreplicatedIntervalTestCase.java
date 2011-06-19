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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.MutableObject;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of handling of ClusteredSession.maxUnreplicatedInterval. This base test is run with SESSION granularity.
 *
 * @author Brian Stansberry
 */
public class SessionBasedMaxUnreplicatedIntervalTestCase extends InfinispanCacheContainerTest {
    protected static long testId = System.currentTimeMillis();

    protected Logger log = Logger.getLogger(getClass());

    protected DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    protected Map<String, Object> allAttributes;
    protected Map<String, Object> immutables;
    protected Map<String, Object> mutables;

    protected SessionBasedMaxUnreplicatedIntervalTestCase(int containers, boolean local, boolean passivate, Boolean totalReplication) {
        super(containers, local, passivate, totalReplication);
    }

    public SessionBasedMaxUnreplicatedIntervalTestCase() {
        this(2, false, false, null);
    }

    @Before
    @Override
    public void start() throws Exception {
        super.start();
        allAttributes = new HashMap<String, Object>();
        immutables = new HashMap<String, Object>();
        mutables = new HashMap<String, Object>();

        allAttributes.put("IMMUTABLE", "IMMUTABLE");
        immutables.put("IMMUTABLE", "IMMUTABLE");

        MutableObject mo = new MutableObject("MUTABLE");
        allAttributes.put("MUTABLE", mo);
        mutables.put("MUTABLE", mo);

        allAttributes = Collections.unmodifiableMap(allAttributes);
        immutables = Collections.unmodifiableMap(immutables);
        mutables = Collections.unmodifiableMap(mutables);
    }

    @After
    @Override
    public void stop() throws Exception {
        for (DistributableSessionManager<?> manager: managers) {
            if (manager != null) {
                try {
                    manager.stop();
                } catch (Throwable e) {
                    e.printStackTrace(System.err);
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

    /**
     * Tests that a maxUnreplicatedInterval of 1 second prevents inadvertent session expiration. Test makes a read-only request
     * after the maxUnreplicatedInterval has passed, waits long enough for the session to expire on the remote node if the
     * read-only request didn't trigger a timestamp replication, and then accesses the session on the 2nd node to confirm that
     * the session is still alive.
     *
     * @throws Exception
     */
    @Test
    public void testBasicMaxIntervalPreventsExpiration() throws Exception {
        log.info("++++ Starting testBasicMaxIntervalPreventsExpiration ++++");

        maxIntervalPreventsExpirationTest(false);
    }

    /**
     * Tests that the override maxUnreplicatedInterval of 0 prevents inadvertent session expiration. Test makes a read-only
     * request after one second has passed, waits long enough for the session to expire on the remote node if the read-only
     * request didn't trigger a timestamp replication, and then accesses the session on the 2nd node to confirm that the session
     * is still alive.
     *
     * @throws Exception
     */
    @Test
    public void testZeroMaxIntervalPreventsExpiration() throws Exception {
        log.info("++++ Starting testZeroMaxIntervalPreventsExpiration ++++");

        maxIntervalPreventsExpirationTest(true);
    }

    private void maxIntervalPreventsExpirationTest(boolean testZero) throws Exception {
        String warname = String.valueOf(++testId);

        int maxUnrep = testZero ? 0 : 1;

        // A war with a maxInactive of 3 secs and a maxUnreplicated of 0 or 1
        startManagers(warname, 3, maxUnrep);

        // Establish session.
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);

        Thread.sleep(1050);

        // Now make a request that will not trigger replication unless the
        // interval is exceeded
        BasicRequestHandler getHandler = new BasicRequestHandler(immutables.keySet(), false);
        SessionTestUtil.invokeRequest(managers[0], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(immutables, getHandler);

        // Sleep long enough that the session will be expired on other server
        // if previous request didn't keep it alive
        Thread.sleep(2000);

        // Fail over and confirm all is well
        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[1], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(allAttributes, getHandler);
    }

    /**
     * Tests that setting a maxUnreplicatedInterval of 1 second prevents timestamp replication during read-only requests during
     * that one second. Test makes a read-only request during the 1 second maxUnreplicatedInterval, which should prevent
     * timestamp replication. Test then waits long enough for the session to be considered expired on the remote node only if
     * the timestamp wasn't replicated. Test fails over to remote node and confirms that the session was expired.
     *
     * @throws Exception
     */
    @Test
    public void testMaxIntervalPreventsReplication() throws Exception {
        log.info("++++ Starting testMaxIntervalPreventsReplication ++++");

        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 3 secs and a maxUnreplicated of 1
        startManagers(warname, 3, 1);

        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        // Sleep less than the maxUnreplicated time so next request shouldn't
        // trigger timestamp repl
        Thread.sleep(500);

        // Now make a request that will not trigger replication unless the
        // interval is exceeded
        BasicRequestHandler getHandler = new BasicRequestHandler(immutables.keySet(), false);
        SessionTestUtil.invokeRequest(managers[0], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(immutables, getHandler);

        // Sleep long enough that the session will be expired on other server
        // if previous request didn't keep it alive
        Thread.sleep(2600);

        // Fail over and confirm the session was expired
        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[1], getHandler, setHandler.getSessionId());

        validateNewSession(getHandler);
    }

    /**
     * Confirms that the "grace period" that maxUnreplicatedInterval adds to the removal of overaged unloaded sessions in remote
     * caches delays their removal.
     *
     * @throws Exception
     */
    @Test
    @org.junit.Ignore
    public void testRemoteExpirationGracePeriod() throws Exception {
        log.info("++++ Starting testRemoteExpirationGracePeriod ++++");

        String warname = String.valueOf(++testId);

        startManagers(warname, 3, 2);

        SetAttributesRequestHandler setHandler1 = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler1, null);
        validateNewSession(setHandler1);

        String id1 = setHandler1.getSessionId();

        SetAttributesRequestHandler setHandler2 = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler2, null);
        validateNewSession(setHandler1);

        String id2 = setHandler2.getSessionId();

        // Overage the sessions
        Thread.sleep(3010);
        // Try to force out the overaged sessions
        managers[1].backgroundProcess();
        // Confirm they are still there
        org.jboss.as.clustering.web.IncomingDistributableSessionData data1 = managers[1].getDistributedCacheManager().getSessionData(id1, false);
        org.jboss.as.clustering.web.IncomingDistributableSessionData data2 = managers[1].getDistributedCacheManager().getSessionData(id2, false);
        assertNotNull(data1);
        assertNotNull(data2);

        // Access one to prove it gets expired once the manager can see its real
        // timestamp
        BasicRequestHandler getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[1], getHandler, setHandler1.getSessionId());
        validateNewSession(getHandler);

        // Sleep past the grace period
        Thread.sleep(2010);
        // The get restored a new fresh session with the first id, but the 2nd
        // one is still there and overaged. Try to force it out
        managers[1].backgroundProcess();
        assertNull(managers[1].getDistributedCacheManager().getSessionData(id2, false));
    }

    protected void startManagers(String warname, int maxInactive, int maxUnreplicated) throws Exception {
        for (int i = 0; i < cacheContainers.length; ++i) {
            JBossWebMetaData metadata = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), true, maxUnreplicated);
            managers[i] = SessionTestUtil.createManager(metadata, warname, maxInactive, cacheContainers[i], "node" + i);
            managers[i].start();
        }
    }

    protected void validateExpectedAttributes(Map<String, Object> expected, BasicRequestHandler handler) {
        assertFalse(handler.isNewSession());

        if (handler.isCheckAttributeNames()) {
            assertEquals(expected.size(), handler.getAttributeNames().size());
        }
        Map<String, Object> checked = handler.getCheckedAttributes();
        assertEquals(expected.size(), checked.size());
        for (Map.Entry<String, Object> entry : checked.entrySet())
            assertEquals(entry.getKey(), expected.get(entry.getKey()), entry.getValue());

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
