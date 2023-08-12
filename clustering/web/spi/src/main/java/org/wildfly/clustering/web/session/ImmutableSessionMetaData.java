/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.time.Instant;

import org.wildfly.clustering.ee.expiration.ExpirationMetaData;

/**
 * Abstraction for immutable meta information about a web session.
 * @author Paul Ferraro
 */
public interface ImmutableSessionMetaData extends ExpirationMetaData {

    /**
     * Indicates whether or not this session was created by the current thread.
     * @return true, if this session is new, false otherwise
     */
    default boolean isNew() {
        return this.getLastAccessStartTime().equals(this.getLastAccessEndTime());
    }

    /**
     * Returns the time this session was created.
     * @return the time this session was created
     */
    Instant getCreationTime();

    /**
     * Returns the start time of the last request to access this session.
     * @return the start time of the last request to access this session.
     */
    Instant getLastAccessStartTime();

    /**
     * Returns the start time of the last request to access this session.
     * @return the start time of the last request to access this session.
     */
    Instant getLastAccessEndTime();

    @Override
    default Instant getLastAccessTime() {
        return this.getLastAccessEndTime();
    }
}
