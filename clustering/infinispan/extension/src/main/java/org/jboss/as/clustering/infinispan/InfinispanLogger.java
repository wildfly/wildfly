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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.service.StartException;

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
     * Logs a warning message indicating that the eager attribute of the transactional element
     * is no longer valid
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "The 'eager' attribute specified on the 'transaction' element of a cache is no longer valid")
    void eagerAttributeDeprecated();

    /**
     * Logs a warning message indicating that the specified topology attribute of the transport element
     * is no longer valid
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "The '%s' attribute specified on the 'transport' element of a cache container is no longer valid" +
                "; use the same attribute specified on the 'transport' element of corresponding JGroups stack instead")
    void topologyAttributeDeprecated(String attribute);

    /**
     * Logs a debug message indicating that named cache container has been installed.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "'%s' cache container installed.")
    void cacheContainerInstalled(String containerName);

    /**
     * Creates an exception indicating a failure to resolve the outbound socket binding represented by the
     * {@code binding} parameter.
     *
     * @param cause the cause of the error.
     * @param binding the outbound socket binding.
     *
     * @return a {@link org.jboss.as.controller.persistence.ConfigurationPersistenceException} for the error.
     */
    @Message(id = 8, value = "Could not resolve destination address for outbound socket binding named '%s'")
    InjectionException failedToInjectSocketBinding(@Cause UnknownHostException cause, OutboundSocketBinding binding);

    @Message(id = 9, value = "Failed to add %s %s cache to non-clustered %s cache container.")
    StartException transportRequired(CacheMode mode, String cache, String cacheContainer);

    /**
     * Creates an exception indicating an invalid cache store.
     *
     * @param cause          the cause of the error.
     * @param cacheStoreName the name of the cache store.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 10, value = "%s is not a valid cache store")
    OperationFailedException invalidCacheStore(@Cause Throwable cause, String cacheStoreName);

    /**
     * Creates an exception indicating an invalid cache store.
     *
     * @param cacheName     the name of the cache store.
     * @param cacheContainerName the container name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11, value = "%s is not a valid default cache. The %s cache container does not contain a cache with that name")
    IllegalArgumentException invalidDefaultCache(String cacheName, String cacheContainerName);

    /**
     * Creates an exception indicating the an executor property is invalid.
     *
     * @param id         the id of the property.
     * @param properties the properties that were searched.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12, value = "No %s property was specified within the executor properties: %s")
    IllegalStateException invalidExecutorProperty(String id, Properties properties);

    /**
     * Creates an exception indicating the an transport property is invalid.
     *
     * @param id         the id of the property.
     * @param properties the properties that were searched.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 13, value = "No %s property was specified within the transport properties: %s")
    IllegalStateException invalidTransportProperty(String id, Properties properties);

    /**
     * Creates an exception indicating that the cache is aborting after the specified number of retries.
     *
     * @param cause           the cause of the error.
     * @param numberOfRetries the number of retries.
     *
     * @return a {@link RuntimeException}
     */
    @Message(id = 14, value = "Aborting cache operation after %d retries.")
    RuntimeException abortingCacheOperation(@Cause Throwable cause, int numberOfRetries);

    /**
     * Creates an exception indicating the an operation parameter is invalid.
     *
     * @param id         the id of the parameter.
     * @param allowableValues the allowable values for the parameter
     *
     * @return the String.
     */
    @Message(id = 15, value = "Invalid value for parameter %s. Allowable values: %s")
    String invalidParameterValue(String id, String allowableValues);

    /**
     * Creates an exception indicating the a cache store cannot be added as one already exists.
     *
     * @param existingStoreName the store which already exists.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 16, value = "Cache store cannot be created: cache store %s is already defined")
    OperationFailedException cacheStoreAlreadyDefined(String existingStoreName);

    /**
     * Creates an exception indicating the a cache store cannot be added as one already exists.
     *
     * @param propertyKey the name of the property.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 17, value = "Value for property with key %s is not defined")
    OperationFailedException propertyValueNotDefined(String propertyKey);

    /**
     * A message indicating that the resource could not be located.
     *
     * @param resourceName the name of the resource.
     *
     * @return the String message.
     */
    @Message(id = 18, value = "Failed to locate %s")
    String notFound(String resourceName);

    /**
     * A message indicating that the resource could not be parsed.
     *
     * @param url the name of the resource.
     *
     * @return IllegalStateException instance.
     */
    @Message(id = 19, value = "Failed to parse %s")
    IllegalStateException failedToParse(@Cause Throwable cause, URL url);

    /**
     * Creates an exception indicating unable to remove an alias from an empty list of aliases.
     *
     * @param aliasName the name of the alias.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 21, value = "cannot remove alias % from empty list.")
    OperationFailedException cannotRemoveAliasFromEmptyList(String aliasName);

    /**
     * Creates an exception indicating that an attribute has been deprecated.
     *
     * @param attributeName the name of the deprecated attribute
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 22, value = "Attribute '%s' has been deprecated.")
    OperationFailedException attributeDeprecated(String attributeName);

    @Message(id = 23, value = "Attribute 'segments' is an expression and therefore cannot be translated to legacy attribute 'virtual-nodes'. This resource will need to be ignored on that host.")
    String virtualNodesDoesNotSupportExpressions();

    @Message(id = 26, value = "Attribute 'virtual nodes' is an expression and therefore cannot be translated to attribute 'segments'.")
    String segmentsDoesNotSupportExpressions();

    @Message(id = 27, value = "Could not determine 'stack' attribute from JGroups subsystem")
    String indeterminiteStack();
}
