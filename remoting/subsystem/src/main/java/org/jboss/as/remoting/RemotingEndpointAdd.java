/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.remoting;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class RemotingEndpointAdd extends AbstractAddStepHandler {

    public RemotingEndpointAdd() {
        super(RemotingEndpointResource.INSTANCE.getAttributes());
    }

    @Override
    protected Resource createResource(OperationContext context) {
        // If the resource is already there but empty, just use it
        // We do this because RemotingSubsystemAdd/WorkerThreadPoolVsEndpointHandler will end up adding a resource if
        // one isn't added in the same op. So if a user adds one in a separate op, we're forgiving about it.
        // Mostly we do this to allow transformers tests to pass which call ModelTestUtils.checkFailedTransformedBootOperations
        // which calls this OSH in a separate op from the one that calls RemotingSubystemAdd
        try {
            Resource existing = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            ModelNode existingModel = existing.getModel();
            if (!existingModel.isDefined()) {
                return existing;
            } else {
                boolean undefined = true;
                for (Property prop : existingModel.asPropertyList()) {
                    if (prop.getValue().isDefined()) {
                        undefined = false;
                        break;
                    }
                }
                if (undefined) {
                    return existing;
                }
            }
        } catch (NoSuchResourceException ignored) {
            //
        }
        return super.createResource(context);
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);

        PathAddress pa = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        context.addStep(Util.createOperation("validate-endpoint", pa.subAddress(0, pa.size() - 1)),
                WorkerThreadPoolVsEndpointHandler.INSTANCE, OperationContext.Stage.MODEL);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.getAttachment(RemotingSubsystemAdd.RUNTIME_KEY) == null) {
            // We're not running in the same op set as RemotingSubsystemAdd
            // See if the config has changed from the default; if so reload is needed
            boolean reload = false;
            for (AttributeDefinition ad : RemotingEndpointResource.INSTANCE.getAttributes()) {
                ModelNode node = model.get(ad.getName());
                if (node.isDefined()) {
                    ModelNode deflt = ad.getDefaultValue();
                    if (!node.equals(deflt)) {
                        reload = true;
                        break;
                    }
                }
            }
            if (reload) {
                context.reloadRequired();
                // Signal rollbackRuntime
                context.attach(RemotingSubsystemAdd.RUNTIME_KEY, Boolean.TRUE);
            }
        }
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        Boolean revert = context.getAttachment(RemotingSubsystemAdd.RUNTIME_KEY);
        if (revert != null && revert.booleanValue()) {
            context.revertReloadRequired();
        }
    }
}
