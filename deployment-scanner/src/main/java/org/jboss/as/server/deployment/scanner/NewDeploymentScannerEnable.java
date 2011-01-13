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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
public class NewDeploymentScannerEnable  implements ModelUpdateOperationHandler, RuntimeOperationHandler {

    static final NewDeploymentScannerEnable INSTANCE = new NewDeploymentScannerEnable();

    private NewDeploymentScannerEnable() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.require(ADDRESS);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("disable-scanner");
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> controller = updateContext.getServiceRegistry().getService(DeploymentScannerService.getServiceName(name));
            if(controller == null) {
                resultHandler.handleFailed(new ModelNode().set("scanner not configured"));
            } else {
                try {
                    final DeploymentScanner scanner = (DeploymentScanner) controller.getValue();
                    scanner.startScanner();
                    // update the model
                    context.getSubModel().get(CommonAttributes.SCAN_ENABLED).set(true);
                    resultHandler.handleResultComplete(compensatingOperation);
                } catch (Throwable t) {
                    resultHandler.handleFailed(getFailureResult(t));
                }
            }
        }

        return Cancellable.NULL;
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