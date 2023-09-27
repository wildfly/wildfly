/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.distribution;

/**
 * Facility for determining the primary ownership/location of a given cache key.
 * @author Paul Ferraro
 */
public interface Locality {
    /**
     * Indicates whether the current node is the primary owner of the specified cache key.
     * For local caches, this method will always return true.
     * @param key a cache key
     * @return true, if the current node is the primary owner of the specified cache key, false otherwise
     */
    boolean isLocal(Object key);
}
