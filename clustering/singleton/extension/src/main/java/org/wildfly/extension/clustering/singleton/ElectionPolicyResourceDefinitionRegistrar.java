/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.extension.clustering.singleton.election.OutboundSocketBindingPreference;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition of an election policy.
 * @author Paul Ferraro
 */
public abstract class ElectionPolicyResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<SingletonElectionPolicy>, UnaryOperator<ResourceDescriptor.Builder> {

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SingletonElectionPolicy.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    private static final AttributeMarshaller PREFERENCES_MARSHALLER = new DefaultAttributeMarshaller() {
        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }

        @Override
        protected String asString(ModelNode value) {
            return value.asListOrEmpty().stream().map(ModelNode::asString).collect(Collectors.joining(" "));
        }
    };
    private static final AttributeParser PREFERENCES_PARSER = new AttributeParsers.AttributeElementParser() {
        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            ModelNode list = operation.get(attribute.getName()).setEmptyList();
            for (String value : reader.getElementText().split("\\s+")) {
                list.add(new ModelNode(value));
            }
        }
    };
    private static final String NAME_PREFERENCES_NAME = "name-preferences";
    static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> SOCKET_BINDING_PREFERENCES = new CapabilityReferenceListAttributeDefinition.Builder<>("socket-binding-preferences", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .setRequired(false)
            .setAlternatives(NAME_PREFERENCES_NAME)
            .setAttributeMarshaller(PREFERENCES_MARSHALLER)
            .setAttributeParser(PREFERENCES_PARSER)
            .build();
    static final AttributeDefinition NAME_PREFERENCES = new StringListAttributeDefinition.Builder(NAME_PREFERENCES_NAME)
            .setAllowExpression(true)
            .setRequired(false)
            .setAlternatives(SOCKET_BINDING_PREFERENCES.getName())
            .setAttributeMarshaller(PREFERENCES_MARSHALLER)
            .setAttributeParser(PREFERENCES_PARSER)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private final ResourceRegistration registration;

    ElectionPolicyResourceDefinitionRegistrar(ResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(CAPABILITY)
                .addAttributes(List.of(SOCKET_BINDING_PREFERENCES, NAME_PREFERENCES))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = SingletonSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), ElectionPolicyResourceRegistration.WILDCARD.getPathElement());
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ModelNode> namePreferences = NAME_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        List<ModelNode> socketBindingPreferences = SOCKET_BINDING_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        SingletonElectionPolicy electionPolicy = this.resolve(context, model);
        List<Predicate<GroupMember>> preferences = new LinkedList<>();
        CapabilityServiceInstaller.Builder<SingletonElectionPolicy, SingletonElectionPolicy> builder = CapabilityServiceInstaller.builder(CAPABILITY, new Supplier<>() {
            @Override
            public SingletonElectionPolicy get() {
                return electionPolicy.prefer(Collections.unmodifiableList(preferences));
            }
        });
        if (!socketBindingPreferences.isEmpty()) {
            Resource policy = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false);
            String containerName = SingletonPolicyResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getContainerAttribute().resolveModelAttribute(context, policy.getModel()).asString();
            UnaryServiceDescriptor<Void> containerTransportChannel = UnaryServiceDescriptor.of(String.join(".", InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName(), "transport", "jgroups"), Void.class);
            if (context.hasOptionalCapability(containerTransportChannel, containerName, CAPABILITY, SOCKET_BINDING_PREFERENCES)) {
                ServiceDependency<EmbeddedCacheManager> container = ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, containerName);
                builder.requires(container);
                for (ModelNode socketBindingPreference : socketBindingPreferences) {
                    ServiceDependency<OutboundSocketBinding> binding = ServiceDependency.on(OutboundSocketBinding.SERVICE_DESCRIPTOR, socketBindingPreference.asString());
                    builder.requires(binding);
                    preferences.add(new Predicate<GroupMember>() {
                        private final JGroupsTransport transport = (JGroupsTransport) container.get().getCacheManagerConfiguration().transport().jgroups().transport();
                        private final Predicate<GroupMember> preference = new OutboundSocketBindingPreference(binding.get(), this.transport.getChannel());

                        @Override
                        public boolean test(GroupMember member) {
                            return this.preference.test(member);
                        }

                        @Override
                        public String toString() {
                            return this.preference.toString();
                        }
                    });
                }
            }
        }
        if (!namePreferences.isEmpty()) {
            for (ModelNode namePreference : namePreferences) {
                String name = namePreference.asString();
                preferences.add(new Predicate<GroupMember>() {
                    @Override
                    public boolean test(GroupMember member) {
                        return member.getName().equals(name);
                    }

                    @Override
                    public String toString() {
                        return name;
                    }
                });
            }
        }
        return builder.build();
    }
}
