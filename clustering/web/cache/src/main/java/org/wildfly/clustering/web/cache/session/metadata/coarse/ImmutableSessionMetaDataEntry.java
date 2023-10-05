/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Instant;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.expiration.Expiration;

/**
 * @author Paul Ferraro
 */
public interface ImmutableSessionMetaDataEntry extends Expiration {

    /**
     * Returns true, if this is a newly created entry, false otherwise.
     * @return true, if this is a newly created entry, false otherwise.
     */
    boolean isNew();

    /**
     * Returns the time this entry was created.
     * @return the creation time
     */
    Instant getCreationTime();

    /**
     * Returns the last access start time, as an offset of the creation time.
     * @return the last access start time, as an offset of the creation time.
     */
    Supplier<Instant> getLastAccessStartTime();

    /**
     * Returns the last access end time, as an offset of the last access start time.
     * @return the last access end time, as an offset of the last access start time.
     */
    Supplier<Instant> getLastAccessEndTime();
}
