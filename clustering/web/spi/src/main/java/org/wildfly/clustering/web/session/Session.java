/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

/**
 * Represents a web session.
 * @author Paul Ferraro
 */
public interface Session<L> extends ImmutableSession, AutoCloseable {
    /**
     * {@inheritDoc}
     */
    @Override
    SessionMetaData getMetaData();

    /**
     * Invalidates this session.
     * @throws IllegalStateException if this session was already invalidated.
     */
    void invalidate();

    /**
     * {@inheritDoc}
     */
    @Override
    SessionAttributes getAttributes();

    /**
     * Indicates that the application thread is finished with this session.
     * This method is intended to be invoked within the context of a batch.
     */
    @Override
    void close();

    /**
     * Returns the local context of this session.
     * The local context is *not* replicated to other nodes in the cluster.
     * @return a local context
     */
    L getLocalContext();
}
