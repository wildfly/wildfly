/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Restartable;

/**
 * SPI for a distributable session manager.
 * @param <L> the local context type
 * @param <B> the batch type
 * @author Paul Ferraro
 */
public interface SessionManager<L, B extends Batch> extends Restartable, ActiveSessionStatistics {

    /**
     * Returns the session with the specified identifier, or null if none exists.
     * Sessions returned by this method must be closed via {@link Session#close()}.
     * This method is intended to be invoked within the context of a batch.
     * @param id a session identifier
     * @return an existing web session, or null if none exists
     */
    Session<L> findSession(String id);

    /**
     * Creates a session using the specified identifier.
     * Sessions returned by this method must be closed via {@link Session#close()}.
     * This method is intended to be invoked within the context of a batch.
     * @param id a session identifier
     * @return a new web session, or null if a session with the specified identifier already exists.
     */
    Session<L> createSession(String id);

    /**
     * Exposes the batching mechanism used by this session manager.
     * @return a batcher.
     */
    Batcher<B> getBatcher();

    /**
     * Returns the identifiers of those sessions that are active on this node.
     * @return a set of session identifiers.
     */
    Set<String> getActiveSessions();

    /**
     * Returns the identifiers of all sessions on this node, including both active and passive sessions.
     * @return a set of session identifiers.
     */
    Set<String> getLocalSessions();

    /**
     * Returns a read-only view of the session with the specified identifier.
     * This method is intended to be invoked within the context of a batch
     * @param id a unique session identifier
     * @return a read-only session or null if none exists
     */
    ImmutableSession readSession(String id);

    /**
     * The maximum duration of time to wait for the completion of requests before the session manager can be stopped.
     * @return a duration
     */
    Duration getStopTimeout();

    /**
     * Returns the identifier factory of this session manager.
     * @return an identifier factory
     */
    Supplier<String> getIdentifierFactory();
}
