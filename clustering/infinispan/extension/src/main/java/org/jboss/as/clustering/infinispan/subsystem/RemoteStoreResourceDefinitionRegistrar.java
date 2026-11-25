/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a remote store.
 * @author Paul Ferraro
 */
public class RemoteStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<RemoteStoreConfiguration, RemoteStoreConfigurationBuilder> {

    static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("remote-servers", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .setMinSize(1)
            .build();
    static final DurationAttributeDefinition SOCKET_TIMEOUT = DurationAttributeDefinition.builder("socket-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();

    enum Attribute implements AttributeDefinitionProvider {
        CACHE("cache", ModelType.STRING, null),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    RemoteStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.REMOTE;
            }

            @Override
            public InfinispanSubsystemModel getDeprecation() {
                // Deprecated in favour of HotRodStore
                return InfinispanSubsystemModel.VERSION_7_0_0;
            }

            @Override
            public ServiceDependency<RemoteStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String remoteCacheName = Attribute.CACHE.resolveModelAttribute(context, model).asString();
                Duration socketTimeout = SOCKET_TIMEOUT.resolve(context, model);
                boolean tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
                return SOCKET_BINDINGS.resolve(context, model).map(new Function<>() {
                    @Override
                    public RemoteStoreConfigurationBuilder apply(List<OutboundSocketBinding> bindings) {
                        RemoteStoreConfigurationBuilder builder = new ConfigurationBuilder().persistence().addStore(RemoteStoreConfigurationBuilder.class)
                                .segmented(false)
                                .remoteCacheName(remoteCacheName)
                                .socketTimeout(socketTimeout.toMillis())
                                .tcpNoDelay(tcpNoDelay)
                        ;
                        for (OutboundSocketBinding binding : bindings) {
                            builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
                        }
                        return builder;
                    }
                });
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(SOCKET_BINDINGS, SOCKET_TIMEOUT))
                .provideAttributes(EnumSet.allOf(Attribute.class));
    }
}
