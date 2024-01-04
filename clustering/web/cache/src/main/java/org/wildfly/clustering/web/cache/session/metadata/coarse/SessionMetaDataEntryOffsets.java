/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.cache.offset.Offset;

/**
 * Encapsulates session metadata entry offsets.
 * @author Paul Ferraro
 */
public interface SessionMetaDataEntryOffsets {
    /**
     * Returns the session timeout delta, as an offset from the current value.
     * @return the session timeout delta, as an offset from the current value.
     */
    Offset<Duration> getTimeoutOffset();

    /**
     * Returns the last access start time delta, as an offset from the current value.
     * @return the last access start time delta, as an offset from the current value.
     */
    Offset<Instant> getLastAccessStartTimeOffset();

    /**
     * Returns the last access end time delta, as an offset from the current value.
     * @return the last access end time delta, as an offset from the current value.
     */
    Offset<Instant> getLastAccessEndTimeOffset();
}
