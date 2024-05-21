/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Definition of a fork resource.
 * @author Paul Ferraro
 */
public class ForkResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("fork", name);
    }

    private final ResourceServiceConfigurator configurator = new ForkChannelFactoryServiceConfigurator(ChannelResourceDefinition.CHANNEL_FACTORY_CAPABILITY, PathAddress::getParent);
    private final ServiceValueExecutorRegistry<JChannel> registry;

    ForkResourceDefinition(ServiceValueExecutorRegistry<JChannel> registry) {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.registry = registry;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(List.of(ChannelResourceDefinition.CHANNEL_CAPABILITY, ChannelResourceDefinition.CHANNEL_FACTORY_CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new ProtocolResourceRegistrar(this.configurator, new ForkProtocolRuntimeResourceRegistration(this.registry)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = address.getLastElement().getValue();
        String channelName = address.getParent().getLastElement().getValue();

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(this.configurator.configure(context, model));

        ServiceDependency<ForkChannelFactory> channelFactory = ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, channelName).map(ForkChannelFactory.class::cast);
        ServiceDependency<String> clusterName = ServiceDependency.on(ChannelResourceDefinition.CHANNEL_CLUSTER, channelName);
        ChannelServiceConfiguration configuration = new ChannelServiceConfiguration() {
            @Override
            public boolean isStatisticsEnabled() {
                return false;
            }

            @Override
            public org.wildfly.clustering.jgroups.ChannelFactory getChannelFactory() {
                return channelFactory.get();
            }

            @Override
            public String getClusterName() {
                return clusterName.get();
            }

            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                channelFactory.accept(builder);
            }
        };
        installers.add(new ChannelServiceConfigurator(ChannelResourceDefinition.CHANNEL_CAPABILITY, configuration).configure(context, model));
        installers.add(this.registry.capture(ServiceDependency.on(ChannelResourceDefinition.CHANNEL, name)));

        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelBinding(name), ChannelResourceDefinition.CHANNEL_CAPABILITY.getCapabilityServiceName(address)));
        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelFactoryBinding(name), ChannelResourceDefinition.CHANNEL_FACTORY_CAPABILITY.getCapabilityServiceName(address)));

        return ResourceServiceInstaller.combine(installers);
    }
}
