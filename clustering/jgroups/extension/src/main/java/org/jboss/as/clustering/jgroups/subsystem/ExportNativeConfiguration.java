/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.Channel;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;

/**
 * Implements /subsystem=jgroups/stack=X/export-native-configuration() operation.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ExportNativeConfiguration extends AbstractRuntimeOnlyHandler {

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        String stackName = context.getCurrentAddressValue();

        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(stackName);
        try {
            ServiceController<ChannelFactory> controller = ServiceContainerHelper.getService(registry, serviceName);
            ServiceController.Mode mode = controller.getMode();
            controller.setMode(ServiceController.Mode.ACTIVE);
            try {
                ChannelFactory factory = controller.awaitValue();
                // Create a temporary channel, but don't connect it
                try (Channel channel = factory.createChannel(UUID.randomUUID().toString())) {
                    // ProtocolStack.printProtocolSpecAsXML() is very hacky and only works on an uninitialized stack
                    List<Protocol> protocols = channel.getProtocolStack().getProtocols();
                    Collections.reverse(protocols);
                    ProtocolStack stack = new ProtocolStack();
                    stack.addProtocols(protocols);
                    context.getResult().set(stack.printProtocolSpecAsXML());
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            } finally {
                controller.setMode(mode);
            }
        } catch (Exception e) {
            throw new OperationFailedException(e.getLocalizedMessage(), e);
        }
    }
}
