/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class DeploymentChainContributingAddStepHandler extends AddStepHandler {

    private final Consumer<DeploymentProcessorTarget> deploymentChainContributor;

    public DeploymentChainContributingAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler, Consumer<DeploymentProcessorTarget> deploymentChainContributor) {
        super(descriptor, handler);
        this.deploymentChainContributor = deploymentChainContributor;
    }

    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        if (context.isBooting()) {
            context.addStep(new DeploymentChainStep(this.deploymentChainContributor), OperationContext.Stage.RUNTIME);

            super.performRuntime(context, operation, resource);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected final void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        if (context.isBooting()) {
            super.rollbackRuntime(context, operation, resource);
        } else {
            context.revertReloadRequired();
        }
    }
}
