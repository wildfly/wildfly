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

import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.junit.Test;

/**
 * Tests of handling of ClusteredSession.maxUnreplicatedInterval. This version is run with ATTRIBUTE granularity.
 *
 * @author Brian Stansberry
 *
 */
public class AttributeBasedMaxUnreplicatedIntervalTestCase extends SessionBasedMaxUnreplicatedIntervalTestCase {
    public AttributeBasedMaxUnreplicatedIntervalTestCase() {
        super(2, false, false, null);
    }

    @Override
    protected ReplicationGranularity getReplicationGranularity() {
        return ReplicationGranularity.ATTRIBUTE;
    }

    /**
     * A test of the "grace period" that maxUnreplicatedInterval adds to the removal of overaged unloaded sessions in remote
     * caches. Confirms that a session still in the "grace period" doesn't have its cache structure removed.
     *
     * @throws Exception
     */
    @Test
    public void testMaxIntervalPreventsLossOfRemoteState() throws Exception {
        log.info("++++ Starting testMaxIntervalPreventsLossOfRemoteState ++++");

        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 3 secs and a maxUnreplicated of 1
        startManagers(warname, 3, 1);

        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);

        Thread.sleep(250);

        // Now make a request that will not trigger replication but keeps the
        // jbcm0 session alive
        BasicRequestHandler getHandler = new BasicRequestHandler(immutables.keySet(), false);
        SessionTestUtil.invokeRequest(managers[0], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(immutables, getHandler);

        // Sleep long enough that the session will be expired on other server
        // if it doesn't have a maxUnreplicatedInterval grace period
        Thread.sleep(2800);

        // jbcm1 considers the session unmodified for > 3 sec
        // maxInactiveInterval.
        // Try to drive the session out of the jbcm1 cache
        managers[1].backgroundProcess();

        // Replicate just one attribute; see if the other is still in jbcm1
        SetAttributesRequestHandler modifyHandler = new SetAttributesRequestHandler(mutables, false);
        SessionTestUtil.invokeRequest(managers[0], modifyHandler, setHandler.getSessionId());

        // Fail over and confirm all is well. If the session was removed,
        // the last replication of just one attribute won't restore all
        // attributes and we'll have a failure
        getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[1], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(allAttributes, getHandler);
    }

    @Test
    public void testDisabledMaxIntervalReplicatesOnDirtyAttribute() throws Exception {
        log.info("++++ Starting testDisabledMaxIntervalReplicatesOnDirtyAttribute ++++");

        String warname = String.valueOf(++testId);

        // A war with a maxInactive of 2 secs and a maxUnreplicated of -1
        startManagers(warname, 2, -1);

        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(allAttributes, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        validateNewSession(setHandler);

        Thread.sleep(250);

        SetAttributesRequestHandler modifyHandler = new SetAttributesRequestHandler(mutables, false);
        SessionTestUtil.invokeRequest(managers[0], modifyHandler, setHandler.getSessionId());

        Thread.sleep(1760);

        // Fail over and confirm all is well
        BasicRequestHandler getHandler = new BasicRequestHandler(allAttributes.keySet(), false);
        SessionTestUtil.invokeRequest(managers[1], getHandler, setHandler.getSessionId());

        validateExpectedAttributes(allAttributes, getHandler);
    }
}
