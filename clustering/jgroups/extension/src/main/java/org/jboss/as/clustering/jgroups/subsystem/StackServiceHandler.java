/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
