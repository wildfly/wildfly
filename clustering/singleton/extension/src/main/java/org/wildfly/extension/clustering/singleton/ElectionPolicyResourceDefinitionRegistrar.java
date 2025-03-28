/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

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
import org.jgroups.JChannel;
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
 * Definition of an election policy resource.
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
        ServiceDependency<SingletonElectionPolicy> electionPolicyDependency = ServiceDependency.of(electionPolicy);
        if (!socketBindingPreferences.isEmpty()) {
            ServiceDependency<List<OutboundSocketBinding>> bindings = SOCKET_BINDING_PREFERENCES.resolve(context, model);
            Resource policy = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false);
            String containerName = SingletonPolicyResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getContainerAttribute().resolveModelAttribute(context, policy.getModel()).asString();
            UnaryServiceDescriptor<JChannel> containerTransportChannel = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.transport.channel", JChannel.class);
            if (context.hasOptionalCapability(containerTransportChannel, containerName, CAPABILITY, SOCKET_BINDING_PREFERENCES)) {
                ServiceDependency<JChannel> channel = ServiceDependency.on(containerTransportChannel, containerName);
                electionPolicyDependency = bindings.combine(channel, new BiFunction<>() {
                    @Override
                    public SingletonElectionPolicy apply(List<OutboundSocketBinding> bindings, JChannel channel) {
                        List<Predicate<GroupMember>> preferences = new ArrayList<>(bindings.size());
                        for (OutboundSocketBinding binding : bindings) {
                            preferences.add(new OutboundSocketBindingPreference(binding, channel));
                        }
                        return electionPolicy.prefer(preferences);
                    }
                });
            }
        } else if (!namePreferences.isEmpty()) {
            List<Predicate<GroupMember>> preferences = new ArrayList<>(namePreferences.size());
            for (ModelNode namePreference : namePreferences) {
                String name = namePreference.asString();
                preferences.add(new Predicate<>() {
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
            electionPolicyDependency = ServiceDependency.of(electionPolicy.prefer(preferences));
        }
        return CapabilityServiceInstaller.builder(CAPABILITY, electionPolicyDependency).build();
    }
}
