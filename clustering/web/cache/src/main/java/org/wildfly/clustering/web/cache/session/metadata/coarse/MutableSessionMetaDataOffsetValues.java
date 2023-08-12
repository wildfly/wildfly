/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.cache.offset.OffsetValue;

/**
 * Encapsulates the mutable session metadata entry properties, captured as offsets from their current values.
 * @author Paul Ferraro
 */
public interface MutableSessionMetaDataOffsetValues extends MutableSessionMetaDataValues {

    /**
     * Creates a mutable session metadata entry delta from the specified metadata entry.
     * @param entry a session metadata entry
     * @return an object encapsulating the mutable session meta data properties
     */
    static <C> MutableSessionMetaDataOffsetValues from(ContextualSessionMetaDataEntry<C> entry) {
        OffsetValue<Duration> timeout = OffsetValue.from(entry.getTimeout());
        OffsetValue<Instant> lastAccessStartTime = entry.getLastAccessStartTime().rebase();
        OffsetValue<Instant> lastAccessEndTime = entry.getLastAccessEndTime().rebase();
        return new MutableSessionMetaDataOffsetValues() {
            @Override
            public OffsetValue<Duration> getTimeout() {
                return timeout;
            }

            @Override
            public OffsetValue<Instant> getLastAccessStartTime() {
                return lastAccessStartTime;
            }

            @Override
            public OffsetValue<Instant> getLastAccessEndTime() {
                return lastAccessEndTime;
            }
        };
    }

    @Override
    OffsetValue<Duration> getTimeout();

    @Override
    OffsetValue<Instant> getLastAccessStartTime();

    @Override
    OffsetValue<Instant> getLastAccessEndTime();
}
