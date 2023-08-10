/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstraction for meta information about a web session.
 * @author Paul Ferraro
 */
public interface SessionMetaData extends ImmutableSessionMetaData {
    /**
     * Sets the time this session was last accessed.
     * @param the start time of the last request
     * @param the end time of the last request
     */
    void setLastAccess(Instant startTime, Instant endTime);

    /**
     * Set the time interval as a duration, after which this session will expire.
     * @param duration a time duration
     */
    void setTimeout(Duration duration);
}
