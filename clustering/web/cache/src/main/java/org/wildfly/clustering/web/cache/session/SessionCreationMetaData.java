/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;

/**
 * The more static aspects of a session's meta-data.
 * @author Paul Ferraro
 */
public interface SessionCreationMetaData extends ImmutableSessionCreationMetaData, AutoCloseable {
    /**
     * Sets the maximum duration of time this session may remain idle before it will be expired by the session manager.
     * @param a maximum duration of time this session may remain idle before it will be expired by the session manager.
     */
    void setTimeout(Duration duration);

    /**
     * Indicates whether or not this session has been invalidated.
     * @return true, if this session was invalidated, false otherwise.
     */
    boolean isValid();

    /**
     * Invalidates this session.
     * @return true, if this session was previous valid, false otherwise
     */
    boolean invalidate();

    /**
     * Indicates whether or not this session was newly created.
     * @return true, if this session was newly created, false otherwise.
     */
    boolean isNew();

    /**
     * Signals the end of the transient lifecycle of this session, typically triggered at the end of a given request.
     */
    @Override
    void close();
}
