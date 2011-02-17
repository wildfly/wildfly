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

import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Update disabling a {@code DeploymentScanner}.
 *
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerDisable implements ModelUpdateOperationHandler {

    static final DeploymentScannerDisable INSTANCE = new DeploymentScannerDisable();

    private DeploymentScannerDisable() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("enable");
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        // update the model
        context.getSubModel().get(CommonAttributes.SCAN_ENABLED).set(false);

        if (context.getRuntimeContext() != null) {
            final ServiceController<?> controller = context.getRuntimeContext().getServiceRegistry()
                    .getService(DeploymentScannerService.getServiceName(name));
            if (controller == null) {
                throw new OperationFailedException(new ModelNode().set("scanner not configured"));
            } else {
                try {
                    final DeploymentScanner scanner = (DeploymentScanner) controller.getValue();
                    scanner.stopScanner();
                    resultHandler.handleResultComplete();
                } catch (Throwable t) {
                    throw new OperationFailedException(getFailureResult(t));
                }
            }
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    protected ModelNode getFailureResult(Throwable t) {
        final ModelNode node = new ModelNode();
        // todo - define this structure
        node.get("success").set(false);
        do {
            final String message = t.getLocalizedMessage();
            node.get("cause").add(t.getClass().getName(), message != null ? message : "");
            t = t.getCause();
        } while (t != null);
        return node;
    }

}
