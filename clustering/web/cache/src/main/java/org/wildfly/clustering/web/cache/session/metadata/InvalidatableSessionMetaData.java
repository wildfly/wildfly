/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata;

import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * @author Paul Ferraro
 */
public interface InvalidatableSessionMetaData extends SessionMetaData, AutoCloseable {
    /**
     * Indicates whether or not this session is still valid.
     * @return true, if this session is valid, false otherwise
     */
    boolean isValid();

    /**
     * Invalidates the session.
     * @return true, if session was invalidated, false if it was already invalid.
     */
    boolean invalidate();

    /**
     * Signals the end of the transient lifecycle of this session, typically triggered at the end of a given request.
     */
    @Override
    void close();
}
