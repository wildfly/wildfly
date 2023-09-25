/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.expiration;

import java.time.Duration;

/**
 * Describes the expiration criteria for an object.
 * @author Paul Ferraro
 */
public interface Expiration {
    /**
     * The duration of time, after which an idle object should expire.
     * @return the object timeout
     */
    Duration getTimeout();

    /**
     * Indicates whether the associated timeout represents and immortal object,
     * i.e. does not expire
     * @return true, if this object is immortal, false otherwise
     */
    default boolean isImmortal() {
        Duration timeout = this.getTimeout();
        return (timeout == null) || timeout.isZero() || timeout.isNegative();
    }
}
