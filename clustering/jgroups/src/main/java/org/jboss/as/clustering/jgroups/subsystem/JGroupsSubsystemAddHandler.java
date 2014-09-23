/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.msc.InjectedValueServiceBuilder;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;

/**
 * Handler for JGroups subsystem add operations.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat, Inc
 */
public class JGroupsSubsystemAddHandler extends AbstractAddStepHandler {

    JGroupsSubsystemAddHandler(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers) throws OperationFailedException {

        ROOT_LOGGER.activatingSubsystem();

        controllers.addAll(installRuntimeServices(context, operation, model, verificationHandler));
    }

    static Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        ServiceTarget target = context.getServiceTarget();
        Collection<ServiceController<?>> controllers = new LinkedList<>();

        controllers.add(ProtocolDefaultsService.build(target).setInitialMode(ON_DEMAND).install());

        InjectedValueServiceBuilder builder = new InjectedValueServiceBuilder(target);

        String defaultChannel = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.resolveModelAttribute(context, model));
        if ((defaultChannel != null) && !defaultChannel.equals(ChannelService.DEFAULT)) {
            controllers.add(builder.build(ChannelService.getServiceName(ChannelService.DEFAULT), ChannelService.getServiceName(defaultChannel), Channel.class).install());
            controllers.add(builder.build(ConnectedChannelService.getServiceName(ChannelService.DEFAULT), ConnectedChannelService.getServiceName(defaultChannel), Channel.class).install());
            controllers.add(builder.build(ChannelService.getStackServiceName(ChannelService.DEFAULT), ChannelService.getStackServiceName(defaultChannel), ChannelFactory.class).install());

            for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
                Iterator<ServiceName> names = installer.getServiceNames(defaultChannel).iterator();
                for (ServiceName name : installer.getServiceNames(ChannelService.DEFAULT)) {
                    controllers.add(builder.build(name, names.next(), Object.class).install());
                }
            }
        }

        String defaultStack = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.resolveModelAttribute(context, model));
        if ((defaultStack != null) && !defaultStack.equals(ChannelFactoryService.DEFAULT)) {
            controllers.add(builder.build(ChannelFactoryService.getServiceName(ChannelFactoryService.DEFAULT), ChannelFactoryService.getServiceName(defaultStack), ChannelFactory.class).install());
        }
        return controllers;
    }
}
