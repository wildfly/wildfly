/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource:
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=hotrod
 *
 * @author Radoslav Husar
 */
public class HotRodStoreResourceDefinition extends StoreResourceDefinition<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    static final PathElement PATH = pathElement("hotrod");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE_CONFIGURATION("cache-configuration", ModelType.STRING, null),
        REMOTE_CACHE_CONTAINER("remote-cache-container", ModelType.STRING, CapabilityReference.builder(CAPABILITY, HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build()),
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type, CapabilityReferenceRecorder capabilityReference) {
            this.definition = new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setAllowExpression(capabilityReference == null)
                    .setRequired(capabilityReference != null)
                    .setCapabilityReference(capabilityReference)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    HotRodStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), HotRodStoreConfigurationBuilder.class);
    }

    @Override
    public Map.Entry<Map.Entry<Supplier<HotRodStoreConfigurationBuilder>, Consumer<HotRodStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {

        String cacheConfiguration = Attribute.CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        String containerName = Attribute.REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();

        ServiceDependency<RemoteCacheContainer> container = ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, containerName);

        Map.Entry<Map.Entry<Supplier<HotRodStoreConfigurationBuilder>, Consumer<HotRodStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = super.resolve(context, model);
        Supplier<HotRodStoreConfigurationBuilder> builderFactory = entry.getKey().getKey();
        Consumer<HotRodStoreConfigurationBuilder> configurator = entry.getKey().getValue().andThen(new Consumer<>() {
            @Override
            public void accept(HotRodStoreConfigurationBuilder builder) {
                builder.segmented(false)
                        .cacheConfiguration(cacheConfiguration)
                        .remoteCacheContainer(container.get());
            }
        });
        Stream<Consumer<RequirementServiceBuilder<?>>> dependencies = entry.getValue();

        return MapEntry.of(MapEntry.of(builderFactory, configurator), Stream.concat(dependencies, Stream.of(container)));
    }
}
