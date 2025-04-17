/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for the available cache types.
 * @author Paul Ferraro
 */
public enum CacheResourceRegistration implements ResourceRegistration {
    LOCAL("local-cache", CacheMode.LOCAL),
    INVALIDATION("invalidation-cache", CacheMode.INVALIDATION_SYNC),
    REPLICATED("replicated-cache", CacheMode.REPL_SYNC),
    DISTRIBUTED("distributed-cache", CacheMode.DIST_SYNC),
    SCATTERED("scattered-cache", CacheMode.DIST_SYNC) {
        @Override
        InfinispanSubsystemModel getDeprecation() {
            return InfinispanSubsystemModel.VERSION_16_0_0;
        }
    },
    ;
    private final PathElement path;
    private final CacheMode mode;

    CacheResourceRegistration(String key, CacheMode mode) {
        this.path = PathElement.pathElement(key);
        this.mode = mode;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    CacheMode getCacheMode() {
        return this.mode;
    }

    InfinispanSubsystemModel getDeprecation() {
        return null;
    }

    PathElement pathElement(String name) {
        return PathElement.pathElement(this.path.getKey(), name);
    }
}
