/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResourceDescription.Attribute.MODIFICATION_QUEUE_SIZE;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a write-behind store component resource.
 * @author Paul Ferraro
 */
public enum StoreWriteBehindResourceDescription implements StoreWriteResourceDescription {
    INSTANCE;

    enum Attribute implements AttributeDefinitionProvider {
        MODIFICATION_QUEUE_SIZE("modification-queue-size", ModelType.INT, new ModelNode(1024)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = StoreWriteResourceDescription.pathElement("behind");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return ResourceDescriptor.stream(EnumSet.allOf(Attribute.class));
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
