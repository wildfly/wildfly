/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;

/**
 * Encapsulates the configuration of a session management provider.
 * @author Paul Ferraro
 * @param <M> the marshaller factory context
 */
public interface DistributableSessionManagementConfiguration<M> {

    /**
     * Returns the strategy to be used by a session manager to persist session attributes.
     * @return the strategy to be used by a session manager to persist session attributes.
     */
    SessionAttributePersistenceStrategy getAttributePersistenceStrategy();

    /**
     * Returns a function that creates a marshaller for a given context.
     * @return a function that creates a marshaller for a given context.
     */
    Function<M, ByteBufferMarshaller> getMarshallerFactory();

    /**
     * Returns the maximum duration for session to retain in memory at a time, after which it will be passivated.
     * @return the maximum duration for session to retain in memory at a time, after which it will be passivated.
     */
    default Optional<Duration> getIdleThreshold() {
        return Optional.empty();
    }
}
