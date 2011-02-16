/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.RuntimeTask;
import org.jboss.as.server.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation adding a new {@link DeploymentScannerService}.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final DeploymentScannerAdd INSTANCE = new DeploymentScannerAdd();

    private DeploymentScannerAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String path = operation.require(CommonAttributes.PATH).asString();
        final boolean enabled = operation.get(CommonAttributes.SCAN_ENABLED).asBoolean(true);
        final int interval = operation.get(CommonAttributes.SCAN_INTERVAL).asInt(5000);
        final String relativeTo;
        if(operation.has(CommonAttributes.RELATIVE_TO)) {
            relativeTo = operation.get(CommonAttributes.RELATIVE_TO).asString();
        } else {
            relativeTo = null;
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        final ModelNode subModel = context.getSubModel();
        subModel.get(CommonAttributes.PATH).set(path);
        subModel.get(CommonAttributes.SCAN_ENABLED).set(enabled);
        subModel.get(CommonAttributes.SCAN_INTERVAL).set(interval);
        if(relativeTo != null) subModel.get(CommonAttributes.RELATIVE_TO).set(relativeTo);

        if(context instanceof RuntimeOperationContext) {
            RuntimeOperationContext.class.cast(context).executeRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context, ResultHandler resultHandler) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    DeploymentScannerService.addService(serviceTarget, name, relativeTo, path, interval, TimeUnit.MILLISECONDS, enabled);
                    resultHandler.handleResultComplete();
                }
            }, resultHandler);
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

}
