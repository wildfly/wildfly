/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathElement;

/**
 * Describes a replicated cache resource.
 * @author Paul Ferraro
 */
public enum ReplicatedCacheResourceDescription implements SharedStateCacheResourceDescription {
    INSTANCE;

    static PathElement pathElement(String name) {
        return PathElement.pathElement("replicated-cache", name);
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    @Override
    public CacheMode getCacheMode() {
        return CacheMode.REPL_SYNC;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
