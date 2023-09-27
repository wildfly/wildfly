/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ProvidedIdentityGroupServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class NoTransportServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        new NoTransportServiceConfigurator(address).build(target).install();

        new ProvidedIdentityGroupServiceConfigurator(name, LocalGroupServiceConfiguratorProvider.LOCAL).configure(context).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        new ProvidedIdentityGroupServiceConfigurator(name, LocalGroupServiceConfiguratorProvider.LOCAL).remove(context);

        context.removeService(new NoTransportServiceConfigurator(address).getServiceName());
    }
}
