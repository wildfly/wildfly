/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public enum OffHeapMemoryResourceDescription implements MemoryResourceDescription {
    INSTANCE;

    private final PathElement path = MemoryResourceDescription.pathElement("off-heap");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OFF_HEAP;
    }
}
