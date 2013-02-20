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

package org.jboss.as.undertow.session;

import org.jboss.as.clustering.web.DistributedCacheManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.undertow.session.notification.ClusteredSessionNotificationPolicy;
import org.jboss.metadata.web.jboss.ReplicationTrigger;

/**
 * View of a Manager from a ClusteredSession.
 * @author Brian Stansberry
 */
public interface ClusteredSessionManager<O extends OutgoingDistributableSessionData> extends SessionManager {
    /**
     * Get the maximum interval between requests, in seconds, after which a request will trigger replication of the session's
     * metadata regardless of whether the request has otherwise made the session dirty. Such replication ensures that other
     * nodes in the cluster are aware of a relatively recent value for the session's timestamp and won't incorrectly expire an
     * unreplicated session upon failover.
     * <p/>
     * Default value is <code>-1</code>.
     * <p/>
     * The cost of the metadata replication depends on the configured {@link #setReplicationGranularityString(String)
     * replication granularity}. With <code>SESSION</code>, the sesssion's attribute map is replicated along with the metadata,
     * so it can be fairly costly. With other granularities, the metadata object is replicated separately from the attributes
     * and only contains a String, and a few longs, ints and booleans.
     * @return the maximum interval since last replication after which a request will trigger session metadata replication. A
     *         value of <code>0</code> means replicate metadata on every request; a value of <code>-1</code> means never
     *         replicate metadata unless the session is otherwise dirty.
     */
    int getMaxUnreplicatedInterval();

    /**
     * Gets the policy for determining whether the servlet spec notifications related to session events are allowed to be
     * emitted on the local cluster node.
     */
    ClusteredSessionNotificationPolicy getNotificationPolicy();

    /**
     * Gets the policy controlling whether session attribute reads and writes mark the session/attribute as needing replication.
     * @return SET, SET_AND_GET, SET_AND_NON_PRIMITIVE_GET or <code>null</code> if this has not yet been configured.
     */
    ReplicationTrigger getReplicationTrigger();

    /**
     * Gets the <code>DistributedCacheManager</code> through which we interact with the distributed cache.
     */
    DistributedCacheManager<O> getDistributedCacheManager();
}
