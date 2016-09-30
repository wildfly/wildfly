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

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * @author Paul Ferraro
 */
public class StackOperationExecutor implements OperationExecutor<ChannelFactory> {

    @Override
    public ModelNode execute(OperationContext context, Operation<ChannelFactory> operation) throws OperationFailedException {
        String stackName = context.getCurrentAddressValue();

        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, stackName);
        try {
            ServiceController<ChannelFactory> controller = ServiceContainerHelper.getService(registry, serviceName);
            ServiceController.Mode mode = controller.getMode();
            controller.setMode(ServiceController.Mode.ACTIVE);
            try {
                return operation.execute(controller.awaitValue());
            } finally {
                controller.setMode(mode);
            }
        } catch (InterruptedException e) {
            throw new OperationFailedException(e.getLocalizedMessage(), e);
        }
    }
}
