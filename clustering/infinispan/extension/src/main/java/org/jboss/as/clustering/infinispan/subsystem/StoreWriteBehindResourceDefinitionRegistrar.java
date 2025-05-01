/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the write-behind component of a cache store.
 * @author Paul Ferraro
 */
public class StoreWriteBehindResourceDefinitionRegistrar extends StoreWriteResourceDefinitionRegistrar {

    static final AttributeDefinition MODIFICATION_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder("modification-queue-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(1024))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    StoreWriteBehindResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreWriteResourceRegistration.BEHIND;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(MODIFICATION_QUEUE_SIZE));
    }

    @Override
    public ServiceDependency<AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int queueSize = MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder> get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async().enable()
                        .modificationQueueSize(queueSize)
                        ;
            }
        });
    }
}
