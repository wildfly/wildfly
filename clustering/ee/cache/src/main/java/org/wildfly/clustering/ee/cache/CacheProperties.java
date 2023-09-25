/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

/**
 * Exposes a cache configuration as simple high-level properties.
 * @author Paul Ferraro
 */
public interface CacheProperties {
    /**
     * Indicates whether the associated cache requires eager locking for cache reads.
     * @return true, if the cache client should perform reads under lock, false otherwise
     */
    boolean isLockOnRead();

    /**
     * Indicates whether the associated cache uses eager locking for cache writes.
     * @return true, if the cache client should perform writes under lock, false otherwise
     */
    boolean isLockOnWrite();

    /**
     * Indicates whether the mode of this cache requires marshalling of cache values
     * @return true, if cache values need to be marshallable, false otherwise.
     */
    boolean isMarshalling();

    /**
     * Indicates whether cache operations should assume immediate marshalling/unmarshalling of the value.
     * @return true, if the cache client will need to handle passivation/activation notifications on read/write, false otherwise
     */
    boolean isPersistent();

    /**
     * Indicates whether the cache is transactional.
     * @return true, if this cache is transactional, false otherwise.
     */
    boolean isTransactional();
}
