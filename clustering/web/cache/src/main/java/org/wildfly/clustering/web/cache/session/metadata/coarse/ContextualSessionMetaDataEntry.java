/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Instant;

import org.wildfly.clustering.ee.cache.function.Remappable;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.web.cache.Contextual;

/**
 * A contextual session metadata entry.
 * @author Paul Ferraro
 * @param <C> the context type
 */
public interface ContextualSessionMetaDataEntry<C> extends SessionMetaDataEntry, Contextual<C>, Remappable<ContextualSessionMetaDataEntry<C>, SessionMetaDataEntryOffsets> {

    @Override
    default Instant getCreationTime() {
        return this.getLastAccessStartTime().getBasis();
    }

    @Override
    OffsetValue<Instant> getLastAccessStartTime();

    @Override
    OffsetValue<Instant> getLastAccessEndTime();
}
