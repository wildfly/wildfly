/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public enum FileStoreResourceDescription implements StoreResourceDescription<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {
    INSTANCE;

    enum DeprecatedAttribute implements AttributeDefinitionProvider, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        RELATIVE_PATH("path", ModelType.STRING, InfinispanSubsystemModel.VERSION_16_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        RELATIVE_TO("relative-to", ModelType.STRING, InfinispanSubsystemModel.VERSION_16_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReference.builder(CAPABILITY, PathManager.PATH_SERVICE_DESCRIPTOR).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanSubsystemModel deprecation) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(false).setDeprecated(deprecation.getVersion()).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)).build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = PersistenceResourceDescription.pathElement("file");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(DeprecatedAttribute.class)), StoreResourceDescription.super.getAttributes());
    }

    @Override
    public ServiceDependency<SoftIndexFileStoreConfigurationBuilder> resolveStore(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public SoftIndexFileStoreConfigurationBuilder get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().segmented(true);
            }
        });
    }
}
