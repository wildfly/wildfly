/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Registers a resource definition for the heap memory component of a cache.
 * @author Paul Ferraro
 */
public class HeapMemoryResourceDefinitionRegistrar extends MemoryResourceDefinitionRegistrar {

    static final EnumAttributeDefinition<MemorySizeUnit> SIZE_UNIT = new EnumAttributeDefinition.Builder<>(MemoryResourceDefinitionRegistrar.SIZE_UNIT).setAllowedValues(EnumSet.of(MemorySizeUnit.ENTRIES)).build();

    HeapMemoryResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return MemoryResourceRegistration.HEAP;
            }

            @Override
            public StorageType getStorageType() {
                return StorageType.HEAP;
            }

            @Override
            public EnumAttributeDefinition<MemorySizeUnit> getSizeUnitAttribute() {
                return SIZE_UNIT;
            }
        });
    }
}
