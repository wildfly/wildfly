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
import java.util.List;
import java.util.Map;

import org.apache.catalina.Context;
import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.InvalidateSessionRequestHandler;
import org.jboss.as.web.session.mocks.MockClusteredSessionNotificationPolicy;
import org.jboss.as.web.session.mocks.MockHttpSessionAttributeListener;
import org.jboss.as.web.session.mocks.MockHttpSessionListener;
import org.jboss.as.web.session.mocks.RemoveAttributesRequestHandler;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of handling of servlet spec notifications.
 *
 * @author Brian Stansberry
 */
public class ClusteredSessionNotificationPolicyTestCase extends InfinispanCacheContainerTest {
    protected static long testId = System.currentTimeMillis();

    protected Logger log = Logger.getLogger(getClass());

    protected DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    protected Map<String, Object> allAttributes;
    protected Map<String, Object> immutables;
    protected Map<String, Object> mutables;
    protected Map<String, Object> attributes;
    protected SessionSpecListenerAttribute attribute = new SessionSpecListenerAttribute();
    protected Map<String, Object> newAttributes;
    protected SessionSpecListenerAttribute newAttribute = new SessionSpecListenerAttribute();

    protected String origNotificationPolicy;

    public ClusteredSessionNotificationPolicyTestCase() {
        super(2, false, true, null);
    }

    @Override
    @Before
    public void start() throws Exception {
        super.start();

        origNotificationPolicy = System.getProperty("jboss.web.clustered.session.notification.policy");
        System.setProperty("jboss.web.clustered.session.notification.policy",
                MockClusteredSessionNotificationPolicy.class.getName());

        attributes = new HashMap<String, Object>();
        attributes.put("KEY", attribute);
        attributes = Collections.unmodifiableMap(attributes);

        newAttributes = new HashMap<String, Object>();
        newAttributes.put("KEY", newAttribute);
        newAttributes = Collections.unmodifiableMap(newAttributes);
    }

    @After
    @Override
    public void stop() throws Exception {
        if (origNotificationPolicy != null) {
            System.setProperty("jboss.web.clustered.session.notification.policy", origNotificationPolicy);
        } else {
            System.clearProperty("jboss.web.clustered.session.notification.policy");
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

        SessionSpecListenerAttribute.invocations.clear();

        super.stop();
    }

    protected ReplicationGranularity getReplicationGranularity() {
        return ReplicationGranularity.SESSION;
    }

    protected ReplicationTrigger getReplicationTrigger() {
        return ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET;
    }

    @Test
    public void testSessionLifecycleWithNotifications() throws Exception {
        log.info("++++ Starting testSessionLifecycleWithNotifications ++++");
        sessionLifecycleTest(true);
    }

    @Test
    public void testSessionLifecycleWithoutNotifications() throws Exception {
        log.info("++++ Starting testSessionLifecycleWithoutNotifications ++++");
        sessionLifecycleTest(false);
    }

    private void sessionLifecycleTest(boolean notify) throws Exception {
        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 30 mins maxUnreplicated of 0
        this.startManagers(warname, 1800, 1);

        assertTrue(managers[0].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp0 = (MockClusteredSessionNotificationPolicy) managers[0].getNotificationPolicy();
        assertNotNull("capability set", mcsnp0.getClusteredSessionNotificationCapability());
        mcsnp0.setResponse(notify);

        assertTrue(managers[1].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp1 = (MockClusteredSessionNotificationPolicy) managers[1].getNotificationPolicy();
        assertNotNull("capability set", mcsnp1.getClusteredSessionNotificationCapability());
        mcsnp1.setResponse(notify);

        MockHttpSessionListener hsl0 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal0 = new MockHttpSessionAttributeListener();
        Context ctx = (Context) managers[0].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl0 });
        ctx.setApplicationEventListeners(new Object[] { hsal0 });

        MockHttpSessionListener hsl1 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal1 = new MockHttpSessionAttributeListener();
        ctx = (Context) managers[1].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl1 });
        ctx.setApplicationEventListeners(new Object[] { hsal1 });

        // Initial request
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);
        String sessionId = setHandler.getSessionId();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.CREATED, hsl0.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.ADDED, hsal0.invocations.get(0));
            assertEquals(2, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(1));

            validateNoNotifications(null, null, hsl1, hsal1, null);
            clearNotifications(hsl0, hsal0, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Modify attribute request
        setHandler = new SetAttributesRequestHandler(newAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, sessionId);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REPLACED, hsal0.invocations.get(0));
            assertEquals(4, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(1));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(2));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(3));

            validateNoNotifications(hsl0, null, hsl1, hsal1, null);
            clearNotifications(null, hsal0, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Passivate
        Thread.sleep(1100);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(0));

            validateNoNotifications(hsl0, hsal0, hsl1, hsal1, null);
            clearNotifications(null, null, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Remove attribute request
        RemoveAttributesRequestHandler removeHandler = new RemoveAttributesRequestHandler(newAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[0], removeHandler, sessionId);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REMOVED, hsal0.invocations.get(0));
            assertEquals(3, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(1));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(2));

            validateNoNotifications(hsl0, null, hsl1, hsal1, null);
            clearNotifications(null, hsal0, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Failover request
        setHandler = new SetAttributesRequestHandler(attributes, false);
        SessionTestUtil.invokeRequest(managers[1], setHandler, sessionId);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl1.invocations.size());
            assertEquals(MockHttpSessionListener.Type.CREATED, hsl1.invocations.get(0));
            assertEquals(1, hsal1.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.ADDED, hsal1.invocations.get(0));
            assertEquals(2, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(1));

            validateNoNotifications(hsl0, hsal0, null, null, null);
            clearNotifications(null, null, hsl1, hsal1, SessionSpecListenerAttribute.invocations);
        }

        // Passivate
        Thread.sleep(1100);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(0));

            validateNoNotifications(hsl0, hsal0, hsl1, hsal1, null);
            clearNotifications(null, null, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Fail back and invalidate session after changing attribute
        InvalidateSessionRequestHandler invalidateHandler = new InvalidateSessionRequestHandler(newAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[0], invalidateHandler, sessionId);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.DESTROYED, hsl0.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REMOVED, hsal0.invocations.get(0));
            assertEquals(3, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(1));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(2));

            validateNoNotifications(null, null, hsl1, hsal1, null);
            clearNotifications(hsl0, hsal0, null, null, SessionSpecListenerAttribute.invocations);
        }
    }

    @Test
    public void testSessionExpirationWithNotifications() throws Exception {
        log.info("++++ Starting testSessionExpirationWithNotifications ++++");
        sessionExpirationTest(true);
    }

    @Test
    public void testSessionExpirationWithoutNotifications() throws Exception {
        log.info("++++ Starting testSessionExpirationWithoutNotifications ++++");
        sessionExpirationTest(false);
    }

    private void sessionExpirationTest(boolean notify) throws Exception {
        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 2 secs and a maxIdle of 1
        this.startManagers(warname, 2, 1);

        assertTrue(managers[0].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp0 = (MockClusteredSessionNotificationPolicy) managers[0]
                .getNotificationPolicy();
        assertNotNull("capability set", mcsnp0.getClusteredSessionNotificationCapability());
        mcsnp0.setResponse(notify);

        assertTrue(managers[1].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp1 = (MockClusteredSessionNotificationPolicy) managers[1]
                .getNotificationPolicy();
        assertNotNull("capability set", mcsnp1.getClusteredSessionNotificationCapability());
        mcsnp1.setResponse(notify);

        MockHttpSessionListener hsl0 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal0 = new MockHttpSessionAttributeListener();
        Context ctx = (Context) managers[0].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl0 });
        ctx.setApplicationEventListeners(new Object[] { hsal0 });

        MockHttpSessionListener hsl1 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal1 = new MockHttpSessionAttributeListener();
        ctx = (Context) managers[1].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl1 });
        ctx.setApplicationEventListeners(new Object[] { hsal1 });

        // Initial request
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);

        String sessionId = setHandler.getSessionId();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.CREATED, hsl0.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.ADDED, hsal0.invocations.get(0));
            assertEquals(2, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(1));

            validateNoNotifications(null, null, hsl1, hsal1, null);
            clearNotifications(hsl0, hsal0, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Failover request
        setHandler = new SetAttributesRequestHandler(newAttributes, false);
        SessionTestUtil.invokeRequest(managers[1], setHandler, sessionId);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl1.invocations.size());
            assertEquals(MockHttpSessionListener.Type.CREATED, hsl1.invocations.get(0));
            assertEquals(1, hsal1.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REPLACED, hsal1.invocations.get(0));
            assertEquals(4, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(1));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(2));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(3));

            validateNoNotifications(hsl0, hsal0, null, null, null);
            clearNotifications(null, null, hsl1, hsal1, SessionSpecListenerAttribute.invocations);
        }

        // Passivate
        Thread.sleep(1100);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(2, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(1));

            validateNoNotifications(hsl0, hsal0, hsl1, hsal1, null);
            clearNotifications(null, null, null, null, SessionSpecListenerAttribute.invocations);
        }

        // Expire
        Thread.sleep(1000);

        managers[0].backgroundProcess();
        managers[1].backgroundProcess();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.DESTROYED, hsl0.invocations.get(0));
            assertEquals(1, hsl1.invocations.size());
            assertEquals(MockHttpSessionListener.Type.DESTROYED, hsl1.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REMOVED, hsal0.invocations.get(0));
            assertEquals(1, hsal1.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REMOVED, hsal1.invocations.get(0));
            assertEquals(4, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(1));
            assertEquals(SessionSpecListenerAttribute.Type.ACTIVATING, SessionSpecListenerAttribute.invocations.get(2));
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(3));

            validateNoNotifications(null, null, null, null, null);
            clearNotifications(hsl0, hsal0, hsl1, hsal1, SessionSpecListenerAttribute.invocations);
        }
    }

    @Test
    public void testUndeployWithNotifications() throws Exception {
        log.info("++++ Starting testUndeployWithNotifications ++++");
        undeployTest(true);
    }

    @Test
    public void testUndeployWithoutNotifications() throws Exception {
        log.info("++++ Starting testUndeployWithoutNotifications ++++");
        undeployTest(false);
    }

    private void undeployTest(boolean notify) throws Exception {
        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 30 mins and no maxIdle
        this.startManagers(warname, 1800, -1);

        assertTrue(managers[0].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp0 = (MockClusteredSessionNotificationPolicy) managers[0]
                .getNotificationPolicy();
        assertNotNull("capability set", mcsnp0.getClusteredSessionNotificationCapability());
        mcsnp0.setResponse(notify);

        assertTrue(managers[1].getNotificationPolicy() instanceof MockClusteredSessionNotificationPolicy);
        MockClusteredSessionNotificationPolicy mcsnp1 = (MockClusteredSessionNotificationPolicy) managers[1]
                .getNotificationPolicy();
        assertNotNull("capability set", mcsnp1.getClusteredSessionNotificationCapability());
        mcsnp1.setResponse(notify);

        MockHttpSessionListener hsl0 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal0 = new MockHttpSessionAttributeListener();
        Context ctx = (Context) managers[0].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl0 });
        ctx.setApplicationEventListeners(new Object[] { hsal0 });

        MockHttpSessionListener hsl1 = new MockHttpSessionListener();
        MockHttpSessionAttributeListener hsal1 = new MockHttpSessionAttributeListener();
        ctx = (Context) managers[1].getContainer();
        ctx.setApplicationSessionLifecycleListeners(new Object[] { hsl1 });
        ctx.setApplicationEventListeners(new Object[] { hsal1 });

        // Initial request
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.CREATED, hsl0.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.ADDED, hsal0.invocations.get(0));
            assertEquals(2, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.BOUND, SessionSpecListenerAttribute.invocations.get(0));
            assertEquals(SessionSpecListenerAttribute.Type.PASSIVATED, SessionSpecListenerAttribute.invocations.get(1));

            validateNoNotifications(null, null, hsl1, hsal1, null);
            clearNotifications(hsl0, hsal0, null, null, SessionSpecListenerAttribute.invocations);

        }

        managers[0].stop();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            assertEquals(1, hsl0.invocations.size());
            assertEquals(MockHttpSessionListener.Type.DESTROYED, hsl0.invocations.get(0));
            assertEquals(1, hsal0.invocations.size());
            assertEquals(MockHttpSessionAttributeListener.Type.REMOVED, hsal0.invocations.get(0));
            assertEquals(1, SessionSpecListenerAttribute.invocations.size());
            assertEquals(SessionSpecListenerAttribute.Type.UNBOUND, SessionSpecListenerAttribute.invocations.get(0));

            validateNoNotifications(null, null, hsl1, hsal1, null);
            clearNotifications(hsl0, hsal0, null, null, SessionSpecListenerAttribute.invocations);

        }

        managers[1].stop();

        if (!notify) {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        } else {
            validateNoNotifications(hsl0, hsal0, hsl1, hsal1);
        }
    }

    private void validateNoNotifications(MockHttpSessionListener hsl0, MockHttpSessionAttributeListener hsal0,
            MockHttpSessionListener hsl1, MockHttpSessionAttributeListener hsal1) {
        validateNoNotifications(hsl0, hsal0, hsl1, hsal1, SessionSpecListenerAttribute.invocations);
    }

    private void validateNoNotifications(MockHttpSessionListener hsl0, MockHttpSessionAttributeListener hsal0,
            MockHttpSessionListener hsl1, MockHttpSessionAttributeListener hsal1,
            List<SessionSpecListenerAttribute.Type> sspalis) {
        if (hsl0 != null) {
            assertEquals(0, hsl0.invocations.size());
        }
        if (hsal0 != null) {
            assertEquals(0, hsal0.invocations.size());
        }
        if (hsl1 != null) {
            assertEquals(0, hsl1.invocations.size());
        }
        if (hsal1 != null) {
            assertEquals(0, hsal1.invocations.size());
        }

        if (sspalis != null) {
            assertEquals(0, sspalis.size());
        }

        clearNotifications(hsl0, hsal0, hsl1, hsal1, sspalis);
    }

    private void clearNotifications(MockHttpSessionListener hsl0, MockHttpSessionAttributeListener hsal0,
            MockHttpSessionListener hsl1, MockHttpSessionAttributeListener hsal1,
            List<SessionSpecListenerAttribute.Type> sspalis) {

        if (hsl0 != null) {
            hsl0.invocations.clear();
        }
        if (hsal0 != null) {
            hsal0.invocations.clear();
        }
        if (hsl1 != null) {
            hsl1.invocations.clear();
        }
        if (hsal1 != null) {
            hsal1.invocations.clear();
        }

        if (sspalis != null) {
            sspalis.clear();
        }
    }

    protected void startManagers(String warName, int maxInactive, int maxIdle) throws Exception {
        for (int i = 0; i < cacheContainers.length; ++i) {
            JBossWebMetaData metaData = SessionTestUtil.createWebMetaData(getReplicationGranularity(), getReplicationTrigger(), -1, (i == 0) ? (maxIdle > 0) : true, maxIdle, -1, false, 0);
            managers[i] = SessionTestUtil.createManager(metaData, warName, maxInactive, cacheContainers[i], null);
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
