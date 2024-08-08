/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Supplier;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource description for the addressable resource
 *
 *    /subsystem=infinispan/cache-container=X/cache=Y/store=Z/write-behind=WRITE_BEHIND
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreWriteBehindResourceDefinition extends StoreWriteResourceDefinition {

    static final PathElement PATH = pathElement("behind");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
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
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    StoreWriteBehindResourceDefinition() {
        super(PATH, descriptor -> descriptor.addAttributes(Attribute.class));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int queueSize = Attribute.MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();

        Supplier<AsyncStoreConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public AsyncStoreConfiguration get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async()
                        .enable()
                        .modificationQueueSize(queueSize)
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
