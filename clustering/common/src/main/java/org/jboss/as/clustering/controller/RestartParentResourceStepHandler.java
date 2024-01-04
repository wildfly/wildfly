/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceHandlerBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Generic operation handler that leverages a {@link ResourceServiceBuilderFactory} to restart a parent resource..
 * @author Paul Ferraro
 */
public class RestartParentResourceStepHandler<T> extends RestartParentResourceHandlerBase {

    private final ResourceServiceConfiguratorFactory parentFactory;

    public RestartParentResourceStepHandler(ResourceServiceConfiguratorFactory parentFactory) {
        super(null);
        this.parentFactory = parentFactory;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime();
    }

    @Override
    protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        this.parentFactory.createServiceConfigurator(parentAddress).configure(context, parentModel).build(context.getServiceTarget()).install();
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return this.parentFactory.createServiceConfigurator(parentAddress).getServiceName();
    }

    @Override
    protected PathAddress getParentAddress(PathAddress address) {
        return address.getParent();
    }
}
