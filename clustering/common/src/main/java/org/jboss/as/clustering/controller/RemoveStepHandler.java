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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Generic remove operation step handler that delegates service removal/recovery to a dedicated {@link ResourceServiceHandler}
 * and recursively removes any child resources and their associated services.
 * @author Paul Ferraro
 */
public class RemoveStepHandler extends AbstractRemoveStepHandler implements Registration {

    private final ResourceDescriptionResolver resolver;
    private final ResourceServiceHandler handler;

    public RemoveStepHandler(ResourceDescriptionResolver resolver, ResourceServiceHandler handler) {
        this.resolver = resolver;
        this.handler = handler;
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        if (resource.getChildTypes().isEmpty()) {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
        } else {
            // First call remove operations for children
            for (String childType : resource.getChildTypes()) {
                for (final Resource.ResourceEntry entry : resource.getChildren(childType)) {
                    if (!entry.isRuntime()) {
                        PathElement path = entry.getPathElement();
                        OperationStepHandler childHandler = context.getResourceRegistration().getOperationHandler(PathAddress.pathAddress(path), ModelDescriptionConstants.REMOVE);
                        PathAddress childAddress = address.append(path);
                        context.addStep(Util.createRemoveOperation(childAddress), childHandler, OperationContext.Stage.MODEL);
                    }
                }
            }
            // Then remove this resource
            OperationStepHandler handler = new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                    // Remove operations on children may have added further steps, so defer resource removal if necessary
                    if (resource.getChildTypes().isEmpty()) {
                        context.removeResource(PathAddress.EMPTY_ADDRESS);
                    } else {
                        context.addStep(operation, this, OperationContext.Stage.MODEL);
                    }
                }
            };
            context.addStep(operation, handler, OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        this.handler.removeServices(context, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        this.handler.installServices(context, model);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, this.resolver).withFlag(OperationEntry.Flag.RESTART_RESOURCE_SERVICES).build(), this);
    }
}
