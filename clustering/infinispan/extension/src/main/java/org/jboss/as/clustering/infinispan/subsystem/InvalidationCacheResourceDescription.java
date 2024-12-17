/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathElement;

/**
 * Describes an invalidation cache resource.
 * @author Paul Ferraro
 */
public enum InvalidationCacheResourceDescription implements ClusteredCacheResourceDescription {
    INSTANCE;

    static final PathElement pathElement(String name) {
        return PathElement.pathElement("invalidation-cache", name);
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    @Override
    public CacheMode getCacheMode() {
        return CacheMode.INVALIDATION_SYNC;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
