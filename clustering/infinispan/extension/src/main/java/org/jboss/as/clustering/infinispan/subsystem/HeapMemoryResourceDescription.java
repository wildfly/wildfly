/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * Describes a heap memory cache component resource.
 * @author Paul Ferraro
 */
public enum HeapMemoryResourceDescription implements MemoryResourceDescription {
    INSTANCE;

    private final PathElement path = MemoryResourceDescription.pathElement("heap");
    private final EnumAttributeDefinition<MemorySizeUnit> unit = new EnumAttributeDefinition.Builder<>(MemoryResourceDescription.SIZE_UNIT).setAllowedValues(EnumSet.of(MemorySizeUnit.ENTRIES)).build();

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.HEAP;
    }

    @Override
    public EnumAttributeDefinition<MemorySizeUnit> getSizeUnitAttribute() {
        return this.unit;
    }
}
