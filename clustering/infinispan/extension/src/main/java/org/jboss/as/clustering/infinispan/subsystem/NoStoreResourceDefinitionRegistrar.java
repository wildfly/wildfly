/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a placeholder store component of a cache.
 * @author Paul Ferraro
 */
public class NoStoreResourceDefinitionRegistrar extends PersistenceResourceDefinitionRegistrar {

    NoStoreResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.NONE;
            }
        });
    }

    @Override
    public ServiceDependency<PersistenceConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(ConfigurationBuilder::new).map(ConfigurationBuilder::persistence);
    }
}
