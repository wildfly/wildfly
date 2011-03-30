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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation adding a new {@link DeploymentScannerService}.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerAdd implements ModelAddOperationHandler, DescriptionProvider {

    static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    static final DeploymentScannerAdd INSTANCE = new DeploymentScannerAdd();

    private DeploymentScannerAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        final String path = operation.require(CommonAttributes.PATH).asString();
        final Boolean enabled = operation.hasDefined(CommonAttributes.SCAN_ENABLED)
            ? operation.get(CommonAttributes.SCAN_ENABLED).asBoolean()
            : null;
        final Integer interval = operation.hasDefined(CommonAttributes.SCAN_INTERVAL)
            ? operation.get(CommonAttributes.SCAN_INTERVAL).asInt()
            : null;
        final String relativeTo = operation.hasDefined(CommonAttributes.RELATIVE_TO)
            ? operation.get(CommonAttributes.RELATIVE_TO).asString()
            : null;
        final Boolean autoDeployZip =  operation.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED)
                    ? operation.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).asBoolean()
                    : null;
        final Boolean autoDeployExp =  operation.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED)
                    ? operation.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).asBoolean()
                    : null;
        final Long deploymentTimeout =  operation.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT)
                    ? operation.get(CommonAttributes.DEPLOYMENT_TIMEOUT).asLong()
                    : null;

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        final ModelNode subModel = context.getSubModel();
        subModel.get(CommonAttributes.NAME).set(name);
        subModel.get(CommonAttributes.PATH).set(path);
        if (enabled != null) subModel.get(CommonAttributes.SCAN_ENABLED).set(enabled);
        if (interval != null) subModel.get(CommonAttributes.SCAN_INTERVAL).set(interval);
        if (autoDeployZip != null) subModel.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).set(autoDeployZip);
        if (autoDeployExp != null) subModel.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).set(autoDeployExp);
        if(relativeTo != null) subModel.get(CommonAttributes.RELATIVE_TO).set(relativeTo);
        if (deploymentTimeout != null) subModel.get(CommonAttributes.DEPLOYMENT_TIMEOUT).set(deploymentTimeout);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    DeploymentScannerService.addService(serviceTarget, name, relativeTo, path, interval, TimeUnit.MILLISECONDS,
                                                        autoDeployZip, autoDeployExp, enabled, deploymentTimeout);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentSubsystemDescriptions.getScannerAdd(locale);
    }

}
