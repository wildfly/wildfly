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
package org.wildfly.clustering.server.infinispan;

import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.infinispan.notifications.cachelistener.event.Event;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLSV", length = 4)
public interface ClusteringServerLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.server.infinispan";

    /**
     * The root logger.
     */
    ClusteringServerLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringServerLogger.class, ROOT_LOGGER_CATEGORY);

    /* Command dispatcher messages */

    @Message(id = 1, value = "A command dispatcher already exists for %s")
    IllegalArgumentException commandDispatcherAlreadyExists(Object id);

    /* Group messages */

    /* Registry messages */

    @LogMessage(level = WARN)
    @Message(id = 20, value = "Failed to purge %s/%s registry of old registry entries for: %s")
    void registryPurgeFailed(@Cause Throwable e, String containerName, String cacheName, Collection<?> members);

    @LogMessage(level = WARN)
    @Message(id = 21, value = "Failed to notify %s/%s registry listener of %s(%s) event")
    void registryListenerFailed(@Cause Throwable e, String containerName, String cacheName, Event.Type type, Map<?, ?> entries);

    @LogMessage(level = WARN)
    @Message(id = 22, value = "Failed to restore local %s/%s registry entry following network partititon merge")
    void failedToRestoreLocalRegistryEntry(@Cause Throwable cause, String containerName, String cacheName);

    /* Service provider registry messages */

    @LogMessage(level = WARN)
    @Message(id = 30, value = "Failed to notify %s/%s service provider registration listener of new providers: %s")
    void serviceProviderRegistrationListenerFailed(@Cause Throwable e, String containerName, String cacheName, Set<Node> providers);
}
