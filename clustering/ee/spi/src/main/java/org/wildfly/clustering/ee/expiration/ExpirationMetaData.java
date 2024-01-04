/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.expiration;

import java.time.Instant;

/**
 * Describes expiration-related metadata.
 * @author Paul Ferraro
 */
public interface ExpirationMetaData extends Expiration {

    /**
     * Indicates whether or not this object is expired.
     * @return true, if this object has expired, false otherwise.
     */
    default boolean isExpired() {
        if (this.isImmortal()) return false;
        Instant lastAccessedTime = this.getLastAccessTime();
        return (lastAccessedTime != null) ? !lastAccessedTime.plus(this.getTimeout()).isAfter(Instant.now()) : false;
    }

    /**
     * Returns the time this object was last accessed.
     * @return the time this object was last accessed.
     */
    Instant getLastAccessTime();
}
