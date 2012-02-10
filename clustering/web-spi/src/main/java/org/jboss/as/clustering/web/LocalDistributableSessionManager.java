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

import org.jboss.marshalling.ClassResolver;
import org.jboss.metadata.web.jboss.ReplicationConfig;

/**
 * Callback interface to allow the distributed caching layer to invoke upon the local session manager.
 * @author Brian Stansberry
 */
public interface LocalDistributableSessionManager {
    /**
     * Gets whether the webapp is configured for passivation.
     * @return <code>true</code> if passivation is enabled
     */
    boolean isPassivationEnabled();

    /**
     * Returns the unique name of this session manager. Typically composed of host name and context name.
     * @return a unique name
     */
    String getName();

    String getHostName();

    String getContextName();

    /**
     * Returns the name of the session manager's engine. The engine name should be the consistent on all nodes.
     * @return an engine name.
     */
    String getEngineName();

    /**
     * Get the classloader able to load application classes.
     * @return the classloader. Will not return <code>null</code>
     */
    ClassResolver getApplicationClassResolver();

    /**
     * Gets the web application metadata.
     * @return the metadata. will not return <code>null</code>
     */
    ReplicationConfig getReplicationConfig();

    /**
     * Notifies the manager that a session in the distributed cache has been invalidated
     * @param realId the session id excluding any jvmRoute
     */
    void notifyRemoteInvalidation(String realId);

    /**
     * Callback from the distributed cache notifying of a local modification to a session's attributes. Meant for use with FIELD
     * granularity, where the session may not be aware of modifications.
     * @param realId the session id excluding any jvmRoute
     */
    void notifyLocalAttributeModification(String realId);

    /**
     * Notification that a previously passivated session has been activated.
     */
    void sessionActivated();

    /**
     * Callback from the distributed cache to notify us that a session has been modified remotely.
     * @param realId the session id, without any trailing jvmRoute
     * @param dataOwner the owner of the session. Can be <code>null</code> if the owner is unknown.
     * @param distributedVersion the session's version per the distributed cache
     * @param timestamp the session's timestamp per the distributed cache
     * @param metadata the session's metadata per the distributed cache
     */
    boolean sessionChangedInDistributedCache(String realId, String dataOwner, int distributedVersion, long timestamp, DistributableSessionMetadata metadata);

    /**
     * Returns the jvm route of this node.
     * @return a jvm route
     */
    String getJvmRoute();
}