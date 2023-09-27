/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Simple {@link ResourceServiceHandler} that installs/removes a single service via a {@link ResourceServiceConfiguratorFactory}.
 * @author Paul Ferraro
 */
public class SimpleResourceServiceHandler implements ResourceServiceHandler {

    private ResourceServiceConfiguratorFactory factory;

    public SimpleResourceServiceHandler(ResourceServiceConfiguratorFactory factory) {
        this.factory = factory;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        this.factory.createServiceConfigurator(context.getCurrentAddress()).configure(context, model).build(context.getServiceTarget()).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        context.removeService(this.factory.createServiceConfigurator(context.getCurrentAddress()).getServiceName());
    }
}
