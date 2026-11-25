/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for memory components of a cache configuration.
 * @author Paul Ferraro
 */
public enum MemoryResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    HEAP("heap"),
    OFF_HEAP("off-heap"),
    ;
    private final PathElement path;

    MemoryResourceRegistration(String value) {
        this.path = PathElement.pathElement("memory", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
