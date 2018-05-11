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

import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * @author Paul Ferraro
 */
public class StackServiceHandler extends SimpleResourceServiceHandler {

    StackServiceHandler(ResourceServiceConfiguratorFactory factory) {
        super(factory);
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        super.installServices(context, model);

        String name = context.getCurrentAddressValue();

        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name)).build(context.getServiceTarget()).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        super.removeServices(context, model);
    }
}
