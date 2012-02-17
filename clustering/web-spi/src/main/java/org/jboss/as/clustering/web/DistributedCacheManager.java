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

package org.jboss.as.clustering.web;

import java.util.Map;

/**
 * SPI implemented by the distributed caching layer.
 * @author Brian Stansberry
 */
public interface DistributedCacheManager<T extends OutgoingDistributableSessionData> {
    /**
     * Starts the distributed caching layer.
     */
    void start();

    /**
     * Stops the distributed caching layer.
     */
    void stop();

    /**
     * Gets the BatchingManager.
     */
    BatchingManager getBatchingManager();

    /**
     * Notification to the distributed cache that a session has been newly created.
     * @param realId the session id with any appended jvmRoute info removed
     */
    void sessionCreated(String realId);

    /**
     * Store or update a session in the distributed cache.
     * @param sessionData the session
     */
    void storeSessionData(T sessionData);

    /**
     * Globally remove a session from the distributed cache.
     * @param realId the session's id, excluding any jvmRoute
     */
    void removeSession(String realId);

    /**
     * Remove a session from the distributed cache on this node only.
     * @param realId the session's id, excluding any jvmRoute
     */
    void removeSessionLocal(String realId);

    /**
     * Remove a non-locally active session from the distributed cache, but on this node only.
     * @param realId the session's id, excluding any jvmRoute
     * @param dataOwner identifier of node where the session is active
     */
    void removeSessionLocal(String realId, String dataOwner);

    /**
     * Evict a session from the in-memory portion of the distributed cache, on this node only.
     * @param realId the session's id, excluding any jvmRoute
     */
    void evictSession(String realId);

    /**
     * Evict a non-locally-active session from the in-memory portion of the distributed cache, on this node only.
     * @param realId the session's id, excluding any jvmRoute
     * @param dataOwner identifier of node where the session is active
     */
    void evictSession(String realId, String dataOwner);

    /**
     * Get the {@link IncomingDistributableSessionData} that encapsulates the distributed cache's information about the given
     * session.
     * @param realId the session's id, excluding any jvmRoute
     * @param initialLoad <code>true</code> if this is the first access of this session's data on this node
     * @return the session data
     */
    IncomingDistributableSessionData getSessionData(String realId, boolean initialLoad);

    /**
     * Get the {@link IncomingDistributableSessionData} that encapsulates the distributed cache's information about the given
     * session.
     * @param realId the session's id, excluding any jvmRoute
     * @param dataOwner identifier of node where the session is active; <code>null</code> if locally active or location where
     *        active is unknown
     * @param includeAttributes should {@link IncomingDistributableSessionData#providesSessionAttributes()} return <code>true</code>?
     * @return the session data
     */
    IncomingDistributableSessionData getSessionData(String realId, String dataOwner, boolean includeAttributes);

    /**
     * Gets the ids of all sessions in the underlying cache.
     * @return Map<String, String> containing all of the session ids of sessions in the cache (with any jvmRoute removed) as
     *         keys, and the identifier of the data owner for the session as value (or a <code>null</code> value if buddy
     *         replication is not enabled.) Will not return <code>null</code>.
     */
    Map<String, String> getSessionIds();

    /**
     * Gets whether the underlying cache supports passivation.
     */
    boolean isPassivationEnabled();

    /**
     * Toggles whether or not to force cache into synchronous mode.
     * @param forceSynchronous true, if cache should force synchronous mode, false otherwise
     */
    void setForceSynchronous(boolean forceSynchronous);

    /**
     * Returns the session ownership support for this distributed cache manager.
     * @return the session ownership support, or null if session ownership is not supported.
     */
    SessionOwnershipSupport getSessionOwnershipSupport();

    /**
     * Indicates whether a session with the specified identifier will cache locally.
     * @param sessionId a session identifier
     * @return true, if a session with the specified identifier will cache locally, false otherwise.
     */
    boolean isLocal(String sessionId);

    /**
     * Returns the jvm route of a node on which the specified session id is cached.
     * @param sessionId a session identifier
     * @return the jvm route of a node
     */
    String locate(String sessionId);
}
