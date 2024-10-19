/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a remote store cache component resource.
 * @deprecated Use {@link org.jboss.as.clustering.infinispan.subsystem.HotRodStoreResourceDescription} instead.
 */
@Deprecated
public enum RemoteStoreResourceDescription implements StoreResourceDescription<RemoteStoreConfiguration, RemoteStoreConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = PersistenceResourceDescription.pathElement("remote");

    static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("remote-servers", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .setMinSize(1)
            .build();

    enum Attribute implements AttributeDefinitionProvider {
        CACHE("cache", ModelType.STRING, null),
        SOCKET_TIMEOUT("socket-timeout", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(1))),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
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

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public InfinispanSubsystemModel getDeprecation() {
        return InfinispanSubsystemModel.VERSION_7_0_0;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.concat(Stream.of(SOCKET_BINDINGS), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class))), StoreResourceDescription.super.getAttributes());
    }

    @Override
    public ServiceDependency<RemoteStoreConfigurationBuilder> resolveStore(OperationContext context, ModelNode model) throws OperationFailedException {
        String remoteCacheName = Attribute.CACHE.resolveModelAttribute(context, model).asString();
        long socketTimeout = Attribute.SOCKET_TIMEOUT.resolveModelAttribute(context, model).asLong();
        boolean tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        return SOCKET_BINDINGS.resolve(context, model).map(new Function<>() {
            @Override
            public RemoteStoreConfigurationBuilder apply(List<OutboundSocketBinding> bindings) {
                RemoteStoreConfigurationBuilder builder = new ConfigurationBuilder().persistence().addStore(RemoteStoreConfigurationBuilder.class)
                        .segmented(false)
                        .remoteCacheName(remoteCacheName)
                        .socketTimeout(socketTimeout)
                        .tcpNoDelay(tcpNoDelay)
                ;
                for (OutboundSocketBinding binding : bindings) {
                    builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
                }
                return builder;
            }
        });
    }
}
