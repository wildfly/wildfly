/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.Function;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public class HotRodStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    static final CapabilityReferenceAttributeDefinition<RemoteCacheContainer> REMOTE_CACHE_CONTAINER = new CapabilityReferenceAttributeDefinition.Builder<>("remote-cache-container", CapabilityReference.builder(CAPABILITY, HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build()).setRequired(true).build();
    static final AttributeDefinition REMOTE_CACHE_CONFIGURATION = new SimpleAttributeDefinitionBuilder("cache-configuration", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    HotRodStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.HOTROD;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return Configurator.super.apply(builder).addAttributes(List.of(REMOTE_CACHE_CONTAINER, REMOTE_CACHE_CONFIGURATION));
            }

            @Override
            public ServiceDependency<HotRodStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String cacheConfiguration = REMOTE_CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
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
        });
    }
}
