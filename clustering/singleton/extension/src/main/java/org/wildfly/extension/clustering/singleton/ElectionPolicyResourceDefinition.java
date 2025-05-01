/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.singleton.election.OutboundSocketBindingPreference;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of an election policy resource.
 * @author Paul Ferraro
 */
public abstract class ElectionPolicyResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<SingletonElectionPolicy> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("election-policy", value);
    }

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SingletonElectionPolicy.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        NAME_PREFERENCES("name-preferences", "socket-binding-preferences"),
        SOCKET_BINDING_PREFERENCES("socket-binding-preferences", "name-preferences") {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
                        ;
            }
        }
        ;
        private final AttributeDefinition definition;

        Attribute(String name, String alternative) {
            this.definition = this.apply(new StringListAttributeDefinition.Builder(name).setAllowExpression(true))
                    .setAlternatives(alternative)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
            return builder;
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;

    ElectionPolicyResourceDefinition(PathElement path, ResourceDescriptionResolver resolver, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, resolver);
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ModelNode> socketBindingPreferences = Attribute.SOCKET_BINDING_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        List<ModelNode> namePreferences = Attribute.NAME_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        List<Predicate<GroupMember>> preferences = new ArrayList<>(socketBindingPreferences.size() + namePreferences.size());
        List<Consumer<RequirementServiceBuilder<?>>> dependencies = new ArrayList<>(socketBindingPreferences.size());
        if (!socketBindingPreferences.isEmpty()) {
            Resource policy = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false);
            String containerName = SingletonPolicyResourceDefinition.CACHE_ATTRIBUTE_GROUP.getContainerAttribute().resolveModelAttribute(context, policy.getModel()).asString();
            UnaryServiceDescriptor<JChannel> containerTransportChannel = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.transport.channel", JChannel.class);
            if (context.hasOptionalCapability(containerTransportChannel, containerName, CAPABILITY, Attribute.SOCKET_BINDING_PREFERENCES.getDefinition())) {
                ServiceDependency<JChannel> channel = ServiceDependency.on(containerTransportChannel, containerName);
                for (ModelNode preference : socketBindingPreferences) {
                    ServiceDependency<OutboundSocketBinding> binding = ServiceDependency.on(OutboundSocketBinding.SERVICE_DESCRIPTOR, preference.toString());
                    dependencies.add(binding);
                    preferences.add(new OutboundSocketBindingPreference(binding, channel));
                }
            }
        }
        for (ModelNode preference : namePreferences) {
            String name = preference.asString();
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

        SingletonElectionPolicy electionPolicy = this.resolve(context, model);
        return CapabilityServiceInstaller.builder(CAPABILITY, Functions.constantSupplier(electionPolicy.prefer(preferences)))
                .requires(dependencies)
                .build();
    }
}
