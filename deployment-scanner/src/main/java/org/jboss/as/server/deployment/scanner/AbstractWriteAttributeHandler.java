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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Base class for write-attribute handlers that change an installed {@code DeploymentScanner}.
 *
 * @author Brian Stansberry
 */
abstract class AbstractWriteAttributeHandler extends ServerWriteAttributeOperationHandler {

    AbstractWriteAttributeHandler(ParameterValidator valueValidator, ParameterValidator resolvedValueValidator) {
        super(valueValidator, resolvedValueValidator);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
            final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String name = address.getLastElement().getValue();
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(DeploymentScannerService.getServiceName(name));
                    DeploymentScanner scanner = null;
                    if (controller == null) {
                        throw new OperationFailedException(new ModelNode().set("scanner not configured"));
                    } else {
                        scanner = (DeploymentScanner) controller.getValue();
                        updateScanner(scanner, newValue);
                    }

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK && scanner != null) {
                        updateScanner(scanner, currentValue);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        return false;
    }

    protected abstract void updateScanner(DeploymentScanner scanner, ModelNode newValue);
}
