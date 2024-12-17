/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=remote
 * /subsystem=infinispan/cache-container=X/cache=Y/remote-store=REMOTE_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @deprecated Use {@link org.jboss.as.clustering.infinispan.subsystem.HotRodStoreResourceDefinition} instead.
 */
@Deprecated
public class RemoteStoreResourceDefinition extends StoreResourceDefinition<RemoteStoreConfiguration, RemoteStoreConfigurationBuilder> {

    static final PathElement PATH = pathElement("remote");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE("cache", ModelType.STRING, null),
        SOCKET_TIMEOUT("socket-timeout", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(1))),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
        SOCKET_BINDINGS("remote-servers")
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .build();
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMinSize(1)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    RemoteStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), RemoteStoreConfigurationBuilder.class);
        this.setDeprecated(InfinispanSubsystemModel.VERSION_7_0_0.getVersion());
    }

    @Override
    public Map.Entry<Map.Entry<Supplier<RemoteStoreConfigurationBuilder>, Consumer<RemoteStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {

        String remoteCacheName = Attribute.CACHE.resolveModelAttribute(context, model).asString();
        long socketTimeout = Attribute.SOCKET_TIMEOUT.resolveModelAttribute(context, model).asLong();
        boolean tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        List<String> bindingNames = StringListAttributeDefinition.unwrapValue(context, Attribute.SOCKET_BINDINGS.resolveModelAttribute(context, model));
        List<ServiceDependency<OutboundSocketBinding>> bindings = new ArrayList<>(bindingNames.size());
        for (String bindingName : bindingNames) {
            ServiceDependency<OutboundSocketBinding> binding = ServiceDependency.on(OutboundSocketBinding.SERVICE_DESCRIPTOR, bindingName);
            bindings.add(binding);
        }

        Map.Entry<Map.Entry<Supplier<RemoteStoreConfigurationBuilder>, Consumer<RemoteStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = super.resolve(context, model);
        Supplier<RemoteStoreConfigurationBuilder> builderFactory = entry.getKey().getKey();
        Consumer<RemoteStoreConfigurationBuilder> configurator = entry.getKey().getValue().andThen(new Consumer<>() {
            @Override
            public void accept(RemoteStoreConfigurationBuilder builder) {
                builder.segmented(false)
                        .remoteCacheName(remoteCacheName)
                        .socketTimeout(socketTimeout)
                        .tcpNoDelay(tcpNoDelay)
                        ;
                for (Supplier<OutboundSocketBinding> dependency : bindings) {
                    OutboundSocketBinding binding = dependency.get();
                    builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
                }
            }
        });
        Stream<Consumer<RequirementServiceBuilder<?>>> dependencies = entry.getValue();

        return MapEntry.of(MapEntry.of(builderFactory, configurator), Stream.concat(dependencies, bindings.stream()));
    }
}
