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
import static org.jboss.as.server.deployment.scanner.DeploymentScannerMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Base class for write-attribute handlers that change an installed {@code DeploymentScanner}.
 *
 * @author Brian Stansberry
 */
abstract class AbstractWriteAttributeHandler extends org.jboss.as.controller.AbstractWriteAttributeHandler<DeploymentScanner> {

    AbstractWriteAttributeHandler(AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
            final String attributeName, final ModelNode newValue, final ModelNode currentValue,
            final HandbackHolder<DeploymentScanner> handbackHolder) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(DeploymentScannerService.getServiceName(name));
        if (controller == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.scannerNotConfigured()));
        } else {
            DeploymentScanner scanner = (DeploymentScanner) controller.getValue();
            updateScanner(scanner, newValue);
            handbackHolder.setHandback(scanner);
        }

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, DeploymentScanner handback) throws OperationFailedException {
        updateScanner(handback, valueToRestore.resolve());
    }

    protected abstract void updateScanner(DeploymentScanner scanner, ModelNode newValue);
}
