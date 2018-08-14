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

package org.jboss.as.clustering.infinispan;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Tristan Tarrant
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYCLINF", length = 4)
public interface InfinispanLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.jboss.as.clustering.infinispan";

    /**
     * The root logger.
     */
    InfinispanLogger ROOT_LOGGER = Logger.getMessageLogger(InfinispanLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an informational message indicating the Infinispan subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Infinispan subsystem.")
    void activatingSubsystem();

    /**
     * Logs an informational message indicating that a cache is being started.
     *
     * @param cacheName     the name of the cache.
     * @param containerName the name of the cache container.
     */
    @LogMessage(level = INFO)
    @Message(id = 2, value = "Started %s cache from %s container")
    void cacheStarted(String cacheName, String containerName);

    /**
     * Logs an informational message indicating that a cache is being stopped.
     *
     * @param cacheName     the name of the cache.
     * @param containerName the name of the cache container.
     */
    @LogMessage(level = INFO)
    @Message(id = 3, value = "Stopped %s cache from %s container")
    void cacheStopped(String cacheName, String containerName);

    /**
     * Logs a warning message indicating that the specified attribute of the specified element is no longer valid and will be ignored.
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "The '%s' attribute of the '%s' element is no longer supported and will be ignored")
    void attributeDeprecated(String attribute, String element);

    /**
     * Logs a warning message indicating that the specified topology attribute of the transport element
     * is no longer valid
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "The '%s' attribute specified on the 'transport' element of a cache container is no longer valid" +
                "; use the same attribute specified on the 'transport' element of corresponding JGroups stack instead")
    void topologyAttributeDeprecated(String attribute);

    @Message(id = 6, value = "Failed to locate a data source bound to %s")
    OperationFailedException dataSourceJndiNameNotFound(String jndiName);

    @Message(id = 7, value = "Failed to locate data source %s")
    OperationFailedException dataSourceNotFound(String name);

//    /**
//     * Creates an exception indicating a failure to resolve the outbound socket binding represented by the
//     * {@code binding} parameter.
//     *
//     * @param cause the cause of the error.
//     * @param binding the outbound socket binding.
//     *
//     * @return a {@link org.jboss.as.controller.persistence.ConfigurationPersistenceException} for the error.
//     */
//    @Message(id = 8, value = "Could not resolve destination address for outbound socket binding named '%s'")
//    InjectionException failedToInjectSocketBinding(@Cause UnknownHostException cause, OutboundSocketBinding binding);

    /**
     * Creates an exception indicating an invalid cache store.
     *
     * @param cause          the cause of the error.
     * @param cacheStoreName the name of the cache store.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10, value = "%s is not a valid cache store")
    IllegalArgumentException invalidCacheStore(@Cause Throwable cause, String cacheStoreName);

    @Message(id = 27, value = "Could not determine 'stack' attribute from JGroups subsystem")
    String indeterminiteStack();

    @LogMessage(level = WARN)
    @Message(id = 28, value = "Executor configuration '%s' was deprecated and will only be used to support legacy slaves in the domain.")
    void executorIgnored(String executorName);

    @LogMessage(level = INFO)
    @Message(id = 29, value = "Started remote cache container '%s'.")
    void remoteCacheContainerStarted(String remoteCacheContainer);

    @LogMessage(level = INFO)
    @Message(id = 30, value = "Stopped remote cache container '%s'.")
    void remoteCacheContainerStopped(String remoteCacheContainer);

    @Message(id = 31, value = "Specified HotRod protocol version %s does not support creating caches automatically. Cache named '%s' must be already created on the Infinispan Server!")
    HotRodClientException remoteCacheMustBeDefined(String protocolVersion, String remoteCacheName);

    @LogMessage(level = INFO)
    @Message(id = 32, value = "Getting remote cache named '%s'. If it does not exist a new cache will be created from configuration template named '%s'; null value uses default cache configuration on the Infinispan Server.")
    void remoteCacheCreated(String remoteCacheName, String cacheConfiguration);
}
