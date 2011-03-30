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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Operation removing a {@link DeploymentScannerService}.
 *
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerRemove implements ModelRemoveOperationHandler, DescriptionProvider {

    static final String OPERATION_NAME = ModelDescriptionConstants.REMOVE;

    static final DeploymentScannerRemove INSTANCE = new DeploymentScannerRemove();

    private DeploymentScannerRemove() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode subModel = new ModelNode();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(ADD);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(CommonAttributes.PATH).set(subModel.get(CommonAttributes.PATH));
        if (subModel.hasDefined(CommonAttributes.SCAN_ENABLED))
            compensatingOperation.get(CommonAttributes.SCAN_ENABLED).set(subModel.get(CommonAttributes.SCAN_ENABLED));
        if (subModel.hasDefined(CommonAttributes.SCAN_INTERVAL))
            compensatingOperation.get(CommonAttributes.SCAN_INTERVAL).set(subModel.get(CommonAttributes.SCAN_INTERVAL));
        if (subModel.hasDefined(CommonAttributes.RELATIVE_TO))
            compensatingOperation.get(CommonAttributes.RELATIVE_TO).set(subModel.get(CommonAttributes.RELATIVE_TO));
        if (subModel.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED))
            compensatingOperation.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).set(subModel.get(CommonAttributes.AUTO_DEPLOY_ZIPPED));
        if (subModel.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED))
            compensatingOperation.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).set(subModel.get(CommonAttributes.AUTO_DEPLOY_EXPLODED));
        if (subModel.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT))
            compensatingOperation.get(CommonAttributes.DEPLOYMENT_TIMEOUT).set(subModel.get(CommonAttributes.DEPLOYMENT_TIMEOUT));


        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry()
                            .getService(DeploymentScannerService.getServiceName(name));
                    if (controller != null) {
                        controller.addListener(new ResultHandler.ServiceRemoveListener(resultHandler));
                    } else {
                        resultHandler.handleResultComplete();
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }


    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentSubsystemDescriptions.getScannerRemove(locale);
    }
}
