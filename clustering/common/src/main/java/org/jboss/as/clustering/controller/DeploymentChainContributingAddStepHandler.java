/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
