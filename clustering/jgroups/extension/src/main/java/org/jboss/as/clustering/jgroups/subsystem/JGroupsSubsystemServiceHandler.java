/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL;
import static org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition.CAPABILITIES;
import static org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition.CLUSTERING_CAPABILITIES;

import java.util.Map;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Version;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.GroupAliasBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class JGroupsSubsystemServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        ROOT_LOGGER.activatingSubsystem(Version.printVersion());

        ServiceTarget target = context.getServiceTarget();
        PathAddress address = context.getCurrentAddress();

        // Handle case where JGroups subsystem is added to a running server
        // In this case, the Infinispan subsystem may have already registered default group capabilities
        if (context.getProcessType().isServer() && !context.isBooting()) {
            if (context.readResourceFromRoot(address.getParent(),false).hasChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan"))) {
                // Following restart, default group services will be installed by this handler, rather than the infinispan subsystem handler
                context.addStep((ctx, operation) -> {
                    ctx.reloadRequired();
                    ctx.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }, OperationContext.Stage.RUNTIME);
                return;
            }
        }

        new ProtocolDefaultsBuilder().build(target).install();

        String defaultChannel = DEFAULT_CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        if (defaultChannel != null) {
            for (Map.Entry<JGroupsRequirement, Capability> entry : CAPABILITIES.entrySet()) {
                new AliasServiceBuilder<>(entry.getValue().getServiceName(address), entry.getKey().getServiceName(context, defaultChannel), entry.getKey().getType()).build(target).install();
            }

            if (!defaultChannel.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(JndiNameFactory.DEFAULT_LOCAL_NAME), JGroupsRequirement.CHANNEL.getServiceName(context, defaultChannel), JGroupsRequirement.CHANNEL.getType()).build(target).install();
                new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(JndiNameFactory.DEFAULT_LOCAL_NAME), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, defaultChannel), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();
            }

            ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

            for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
                for (CapabilityServiceBuilder<?> builder : provider.getBuilders(registry, null, defaultChannel)) {
                    builder.configure(context).build(target).install();
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String defaultChannel = DEFAULT_CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        if (defaultChannel != null) {
            ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

            for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
                for (ServiceNameProvider builder : provider.getBuilders(registry, null, defaultChannel)) {
                    context.removeService(builder.getServiceName());
                }
            }

            if (!defaultChannel.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
                context.removeService(JGroupsBindingFactory.createChannelBinding(JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
            }

            for (Capability capability : CAPABILITIES.values()) {
                context.removeService(capability.getServiceName(address));
            }
        }

        context.removeService(ProtocolDefaultsBuilder.SERVICE_NAME);
    }
}
