/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.Discovery;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolResourceDefinition<A, P extends Discovery> extends ProtocolResourceDefinition<P> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        OUTBOUND_SOCKET_BINDINGS("socket-bindings") {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name) {
            this.definition = this.apply(new StringListAttributeDefinition.Builder(name)
                    .setRequired(true)
                    .setMinSize(1)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation(Attribute.class))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    ;
        }
    }

    private final Function<InetSocketAddress, A> hostTransformer;

    SocketDiscoveryProtocolResourceDefinition(String name, Function<InetSocketAddress, A> hostTransformer, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), parentServiceConfigurator);
        this.hostTransformer = hostTransformer;
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<P>, ProtocolConfiguration<P>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ServiceDependency<OutboundSocketBinding>> bindings = new LinkedList<>();
        for (String bindingName : StringListAttributeDefinition.unwrapValue(context, Attribute.OUTBOUND_SOCKET_BINDINGS.resolveModelAttribute(context, model))) {
            ServiceDependency<OutboundSocketBinding> binding = ServiceDependency.on(OutboundSocketBinding.SERVICE_DESCRIPTOR, bindingName);
            bindings.add(binding);
        }
        Function<InetSocketAddress, A> hostTransformer = this.hostTransformer;
        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<P> apply(ProtocolConfiguration<P> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public P createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        P protocol = super.createProtocol(stackConfiguration);
                        if (!bindings.isEmpty()) {
                            List<A> initialHosts = new ArrayList<>(bindings.size());
                            for (Supplier<OutboundSocketBinding> dependency : bindings) {
                                OutboundSocketBinding binding = dependency.get();
                                try {
                                    initialHosts.add(hostTransformer.apply(new InetSocketAddress(binding.getResolvedDestinationAddress(), binding.getDestinationPort())));
                                } catch (UnknownHostException e) {
                                    throw JGroupsLogger.ROOT_LOGGER.failedToResolveSocketBinding(e, binding);
                                }
                            }
                            // In the absence of some common interface, we need to use reflection
                            this.setValue(protocol, "initial_hosts", initialHosts);
                        }
                        return protocol;
                    }
                };
            }
        }, new Consumer<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                for (ServiceDependency<OutboundSocketBinding> binding : bindings) {
                    binding.accept(builder);
                }
            }
        });
    }
}
