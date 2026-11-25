/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumeration of cache container resource registrations.
 * @author Paul Ferraro
 */
public enum CacheContainerResourceRegistration implements ResourceRegistration {
    EMBEDDED("cache-container"),
    REMOTE("remote-cache-container"),
    ;
    private final PathElement path;

    CacheContainerResourceRegistration(String key) {
        this.path = PathElement.pathElement(key);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    PathElement pathElement(String value) {
        return PathElement.pathElement(this.path.getKey(), value);
    }
}
