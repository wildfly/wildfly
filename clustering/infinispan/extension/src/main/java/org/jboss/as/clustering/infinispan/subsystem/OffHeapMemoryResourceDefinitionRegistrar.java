/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Registers a resource definition for the heap memory component of a cache.
 * @author Paul Ferraro
 */
public class OffHeapMemoryResourceDefinitionRegistrar extends MemoryResourceDefinitionRegistrar {

    OffHeapMemoryResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return MemoryResourceRegistration.OFF_HEAP;
            }

            @Override
            public StorageType getStorageType() {
                return StorageType.OFF_HEAP;
            }
        });
    }
}
