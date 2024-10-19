/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a HotRod cache store resource.
 * @author Paul Ferraro
 */
public enum HotRodStoreResourceDescription implements StoreResourceDescription<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {
    INSTANCE;

    static final CapabilityReferenceAttributeDefinition<RemoteCacheContainer> REMOTE_CACHE_CONTAINER = new CapabilityReferenceAttributeDefinition.Builder<>("remote-cache-container", CapabilityReference.builder(PersistenceResourceDescription.CAPABILITY, HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build()).setRequired(true).build();

    enum Attribute implements AttributeDefinitionProvider {
        CACHE_CONFIGURATION("cache-configuration", ModelType.STRING),
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = PersistenceResourceDescription.pathElement("hotrod");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.concat(Stream.of(REMOTE_CACHE_CONTAINER), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class))), StoreResourceDescription.super.getAttributes());
    }

    @Override
    public ServiceDependency<HotRodStoreConfigurationBuilder> resolveStore(OperationContext context, ModelNode model) throws OperationFailedException {
        String cacheConfiguration = Attribute.CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        return REMOTE_CACHE_CONTAINER.resolve(context, model).map(new Function<>() {
            @Override
            public HotRodStoreConfigurationBuilder apply(RemoteCacheContainer container) {
                return new ConfigurationBuilder().persistence().addStore(HotRodStoreConfigurationBuilder.class)
                        .segmented(false)
                        .remoteCacheContainer(container)
                        .cacheConfiguration(cacheConfiguration);
            }
        });
    }
}
