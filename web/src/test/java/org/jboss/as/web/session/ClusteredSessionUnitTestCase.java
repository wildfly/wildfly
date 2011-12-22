/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat Middleware, Inc. and individual contributors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.catalina.Container;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.web.session.mocks.MockDistributedCacheManagerFactory;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.junit.Test;

/**
 * Unit tests of {@link ClusteredSession}.
 *
 * @author Brian Stansberry
 *
 * @version $Revision: $
 */
public class ClusteredSessionUnitTestCase {
    /**
     * Validates the behavior of isOutdated() with respect to returning true until a creation time is set.
     * <p>
     * Note: the use of creation time is a convenience; it's just a field that isn't set at construction but rather after the
     * session is either loaded from the distributed cache or is added as a brand new session.
     *
     * @throws Exception
     */
    @Test
    public void testNewSessionIsOutdated() throws Exception {
        DistributableSessionManager<?> mgr = new DistributableSessionManager<OutgoingDistributableSessionData>(new MockDistributedCacheManagerFactory(), mock(Container.class), SessionTestUtil.createWebMetaData(10));
        SessionTestUtil.setupContainer("test", null, mgr);
        mgr.start();

        mgr.getReplicationConfig().setReplicationGranularity(ReplicationGranularity.SESSION);
        ClusteredSession<?> sess = (ClusteredSession<?>) mgr.createEmptySession();
        assertTrue(sess.isOutdated());
        sess.setCreationTime(System.currentTimeMillis());
        assertFalse(sess.isOutdated());

        mgr.getReplicationConfig().setReplicationGranularity(ReplicationGranularity.ATTRIBUTE);
        sess = (ClusteredSession<?>) mgr.createEmptySession();
        assertTrue(sess.isOutdated());
        sess.setCreationTime(System.currentTimeMillis());
        assertFalse(sess.isOutdated());
    }
}
