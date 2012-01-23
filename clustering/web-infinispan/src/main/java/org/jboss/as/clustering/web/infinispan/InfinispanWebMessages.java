/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.web.infinispan;

import org.jboss.as.clustering.ClusteringMessages;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.metadata.web.jboss.ReplicationGranularity;

/**
 * Date: 29.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface InfinispanWebMessages extends ClusteringMessages {
    /**
     * The messages
     */
    InfinispanWebMessages MESSAGES = Messages.getBundle(InfinispanWebMessages.class);

    /**
     * Creates an exception indicating an unexpected exception occurred while starting the group communication service
     * for the cluster represented by the {@code clusterName} parameter.
     *
     * @param clusterName the cluster the service was attempting to start on.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10330, value = "Unexpected exception while starting group communication service for %s")
    IllegalStateException errorStartingGroupCommunications(@Cause Throwable cause, String clusterName);

    /**
     * Creates an exception indicating an unexpected exception occurred while starting the lock manager for the cluster
     * represented by the {@code clusterName} parameter.
     *
     * @param clusterName the cluster the service was attempting to start on.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10331, value = "Unexpected exception while starting lock manager for %s")
    IllegalStateException errorStartingLockManager(@Cause Throwable cause, String clusterName);

    /**
     * A message indicating a failure to configure a web application for distributable sessions.
     *
     * @param cacheManagerName the cache manager name.
     * @param sessionCacheName the session cache name.
     *
     * @return the message.
     */
    @Message(id = 10332, value = "Failed to configure web application for <distributable/> sessions.  %s.%s cache requires batching=\"true\".")
    String failedToConfigureWebApp(String cacheManagerName, String sessionCacheName);

    /**
     * Creates an exception indicating a failure to store attributes for the session.
     *
     * @param cause     the cause of the error.
     * @param sessionId the session id.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10333, value = "Failed to load session attributes for session: %s")
    RuntimeException failedToLoadSessionAttributes(@Cause Throwable cause, String sessionId);

    @Message(id = 10334, value = "Failed to store session attributes for session: %s")
    RuntimeException failedToStoreSessionAttributes(@Cause Throwable cause, String sessionId);

    /**
     * Creates an exception indicating an attempt to put a value of the type represented by the {@code typeClassName}
     * parameter into the map.
     *
     * @param typeClassName the type class name.
     * @param map           the map the value was attempted be placed in.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10335, value = "Attempt to put value of type %s into %s entry")
    IllegalArgumentException invalidMapValue(String typeClassName, Object map);

    /**
     * Creates an exception indicating the replication granularity is unknown.
     *
     * @param value the invalid replication granularity.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10336, value = "Unknown replication granularity: %s")
    IllegalArgumentException unknownReplicationGranularity(ReplicationGranularity value);
}
