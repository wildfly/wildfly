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

import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;

public interface DistributableSessionManagerMBean extends SessionManagerMBean {
    /**
     * Gets the value of the attribute with the given key from the given session. If the session is in the distributed store but
     * hasn't been loaded on this node, invoking this method will cause it to be loaded.
     *
     * @param sessionId the id of the session
     * @param key the attribute key
     * @return the value, converted to a String via toString(), or <code>null</code> if the session or key does not exist.
     */
    String getSessionAttribute(String sessionId, String key);

    /**
     * Expires the given session. If the session is in the distributed store but hasn't been loaded on this node, invoking this
     * method will cause it to be loaded.
     *
     * @param sessionId the id of the session
     */
    void expireSession(String sessionId);

    /**
     * Gets the last time the given session was accessed. If the session is in the distributed store but hasn't been loaded on
     * this node, invoking this method will cause it to be loaded.
     *
     * @param sessionId
     * @return the last accessed time, or the empty string if the session doesn't exist.
     */
    String getLastAccessedTime(String sessionId);

    /**
     * Gets the creation time of the given session. If the session is in the distributed store but hasn't been loaded on this
     * node, invoking this method will cause it to be loaded.
     *
     * @param sessionId
     * @return the creation time, or or the empty string if the session doesn't exist.
     */
    String getCreationTime(String sessionId);

    /**
     * Gets the cache config name used to get the underlying cache from a cache manager.
     *
     * @return the config name, or <code>null</code> if this has not yet been configured or the cache was directly injected.
     */
    String getCacheConfigName();

    /**
     * Gets the replication granularity.
     *
     * @return SESSION, ATTRIBUTE or FIELD, or <code>null</code> if this has not yet been configured.
     */
    ReplicationGranularity getReplicationGranularity();

    /**
     * Gets the replication trigger.
     *
     * @return SET, SET_AND_GET, SET_AND_NON_PRIMITIVE_GET or <code>null</code> if this has not yet been configured.
     */
    ReplicationTrigger getReplicationTrigger();

    /**
     * Gets whether JK is being used and special handling of a jvmRoute portion of session ids is needed.
     */
    boolean getUseJK();

    /**
     * Gets the snapshot mode.
     *
     * @return "instant" or "interval"
     */
    SnapshotMode getSnapshotMode();

    /**
     * Gets the number of milliseconds between replications if "interval" mode is used.
     */
    int getSnapshotInterval();

    /**
     * Get the maximum interval between requests, in seconds, after which a request will trigger replication of the session's
     * metadata regardless of whether the request has otherwise made the session dirty. Such replication ensures that other
     * nodes in the cluster are aware of a relatively recent value for the session's timestamp and won't incorrectly expire an
     * unreplicated session upon failover.
     * <p/>
     * Default value is <code>-1</code>.
     * <p/>
     * The cost of the metadata replication depends on the configured {@link #setReplicationGranularityString(String)
     * replication granularity}. With <code>SESSION</code>, the session's attribute map is replicated along with the metadata,
     * so it can be fairly costly. With other granularities, the metadata object is replicated separately from the attributes
     * and only contains a String, and a few longs, ints and booleans.
     *
     * @return the maximum interval since last replication after which a request will trigger session metadata replication. A
     *         value of <code>0</code> means replicate metadata on every request; a value of <code>-1</code> means never
     *         replicate metadata unless the session is otherwise dirty.
     */
    int getMaxUnreplicatedInterval();

    /**
     * Sets the maximum interval between requests, in seconds, after which a request will trigger replication of the session's
     * metadata regardless of whether the request has otherwise made the session dirty.
     *
     * @param maxUnreplicatedInterval the maximum interval since last replication after which a request will trigger session
     *        metadata replication. A value of <code>0</code> means replicate metadata on every request; a value of
     *        <code>-1</code> means never replicate metadata unless the session is otherwise dirty.
     */
    void setMaxUnreplicatedInterval(int maxUnreplicatedInterval);

    /**
     * Lists all session ids known to this manager, including those in the distributed store that have not been accessed on this
     * node.
     *
     * @return a comma-separated list of session ids
     */
    String listSessionIds();

    /**
     * Lists all session ids known to this manager, excluding those in the distributed store that have not been accessed on this
     * node.
     *
     * @return a comma-separated list of session ids
     */
    String listLocalSessionIds();

    /**
     * Gets whether passivation was enabled in jboss-web.xml and in the underlying cache.
     *
     * @return <code>true</code> if passivation is enabled in both jboss-web.xml and in the cache; <code>false</code> otherwise
     */
    boolean isPassivationEnabled();

    /**
     * Gets the number of passivated sessions
     *
     * @return
     */
    long getPassivatedSessionCount();

    /**
     * Gets the highest number of passivated sessions seen.
     *
     * @return
     */
    long getMaxPassivatedSessionCount();

    /**
     * Elapsed time after which an inactive session will be passivated to persistent storage if {@link #isPassivationEnabled()
     * passivation is enabled}.
     *
     * @return
     */
    long getPassivationMaxIdleTime();

    /**
     * Elapsed time after which an inactive session will be passivated to persistent storage if {@link #isPassivationEnabled()
     * passivation is enabled} and the manager needs to passivate sessions early in order to comply with a
     * {@link SessionManagerMBean#getMaxActiveAllowed()} setting.
     *
     * @return
     */
    long getPassivationMinIdleTime();

    /**
     * Gets the number of duplicated session ids generated.
     */
    int getDuplicates();

    /**
     * Sets the number of duplicated session ids generated.
     *
     * @param duplicates the number of duplicates session ids
     */
    void setDuplicates(int duplicates);
}
