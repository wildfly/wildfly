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

package org.jboss.as.clustering;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;

import java.io.Serializable;
import java.util.Properties;

/**
 * Date: 18.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ClusteringMessages {

    /**
     * Creates an exception indicating that the cache is aborting after the specified number of retries.
     *
     * @param cause           the cause of the error.
     * @param numberOfRetries the number of retries.
     *
     * @return a {@link RuntimeException}
     */
    @Message(id = 10215, value = "Aborting cache operation after %d retries.")
    RuntimeException abortingCacheOperation(@Cause Throwable cause, int numberOfRetries);

    /**
     * A message indicating a lock could not be acquired from a cluster.
     *
     * @param lockName the lock name.
     *
     * @return the message.
     */
    @Message(id = 10216, value = "Cannot acquire lock %s from cluster")
    String cannotAcquireLock(Serializable lockName);

    /**
     * Creates an exception indicating a raw throwable was caught on a remote invocation.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10217, value = "Caught raw Throwable on remote invocation")
    RuntimeException caughtRemoteInvocationThrowable(@Cause Throwable cause);

    /**
     * Creates an exception indicating the {@code Object} represented by the {@code value} parameter does not implement
     * the interface represented by the {@code interfaceName} parameter.
     *
     * @param value         the object that does not implement the interface.
     * @param interfaceName the interface name that should be implemented.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10218, value = "%s does not implement %s")
    IllegalArgumentException interfaceNotImplemented(Object value, String interfaceName);

    /**
     * Creates an exception indicating an invalid cache store.
     *
     * @param cause          the cause of the error.
     * @param cacheStoreName the name of the cache store.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10219, value = "%s is not a valid cache store")
    IllegalArgumentException invalidCacheStore(@Cause Throwable cause, String cacheStoreName);

    /**
     * Creates an exception indicating an invalid cache store.
     *
     * @param cacheStoreName     the name of the cache store.
     * @param cacheContainerName the container name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10220, value = "%s is not a valid default cache. The %s cache container does not contain a cache with that name")
    IllegalArgumentException invalidCacheStore(String cacheStoreName, String cacheContainerName);

    /**
     * Creates an exception indicating the an executor property is invalid.
     *
     * @param id         the id of the property.
     * @param properties the properties that were searched.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10221, value = "No %s property was specified within the executor properties: %s")
    IllegalStateException invalidExecutorProperty(String id, Properties properties);

    /**
     * Creates an exception indicating the first method must be called before the second method.
     *
     * @param firstMethod  the first method that should be called.
     * @param secondMethod the second method.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10222, value = "Must call %s before first call to %s")
    IllegalStateException invalidMethodCall(String firstMethod, String secondMethod);

    /**
     * Creates an exception indicating the an transport property is invalid.
     *
     * @param id         the id of the property.
     * @param properties the properties that were searched.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10223, value = "No %s property was specified within the transport properties: %s")
    IllegalStateException invalidTransportProperty(String id, Properties properties);

    /**
     * A message indicating a resource could not be located.
     *
     * @param resource the resource that could not be located.
     *
     * @return the message.
     */
    @Message(id = 10224, value = "Failed to locate %s")
    String notFound(String resource);

    /**
     * Creates an exception indicating a variable is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10225, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the variable represented the {@code name} parameter must be set before invoking
     * the method represented by the {@code methodName}.
     *
     * @param name       the variable name.
     * @param methodName the method name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10226, value = "Must set %s before calling %s")
    IllegalStateException varNotSet(String name, String methodName);
}
