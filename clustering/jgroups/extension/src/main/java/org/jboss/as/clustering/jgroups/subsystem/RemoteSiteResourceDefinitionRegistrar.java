/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource definition for subsystem=jgroups/stack=X/relay=RELAY/remote-site=Y
 *
 * @author Paul Ferraro
 */
public class RemoteSiteResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RemoteSiteConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_CHILD).build();

    static final CapabilityReferenceAttributeDefinition<ChannelConfiguration> CHANNEL_CONFIGURATION = new CapabilityReferenceAttributeDefinition.Builder<>("channel", CapabilityReference.builder(CAPABILITY, ChannelConfiguration.SERVICE_DESCRIPTOR).build()).build();

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    RemoteSiteResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        this.parentRuntimeHandler = parentRuntimeHandler;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(JGroupsResourceRegistration.REMOTE_SITE.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(CHANNEL_CONFIGURATION))
                .addCapability(CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(ResourceOperationRuntimeHandler.configureService(this), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler)))
                .build();

        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(JGroupsResourceRegistration.REMOTE_SITE, resolver).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        ServiceDependency<RemoteSiteConfiguration> site = CHANNEL_CONFIGURATION.resolve(context, model).map(new Function<>() {
            @Override
            public RemoteSiteConfiguration apply(ChannelConfiguration configuration) {
                return new RemoteSiteConfiguration() {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public ChannelFactory getChannelFactory() {
                        return configuration.getChannelFactory();
                    }

                    @Override
                    public String getClusterName() {
                        return configuration.getClusterName();
                    }
                };
            }
        });
        return CapabilityServiceInstaller.builder(CAPABILITY, site).build();
    }}
