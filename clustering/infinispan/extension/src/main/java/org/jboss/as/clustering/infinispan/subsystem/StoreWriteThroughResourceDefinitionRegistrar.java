/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Supplier;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the write-through component of a cache store.
 * @author Paul Ferraro
 */
public class StoreWriteThroughResourceDefinitionRegistrar extends StoreWriteResourceDefinitionRegistrar {

    StoreWriteThroughResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreWriteResourceRegistration.THROUGH;
            }
        });
    }

    @Override
    public ServiceDependency<AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder> get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async().disable();
            }
        });
    }
}
