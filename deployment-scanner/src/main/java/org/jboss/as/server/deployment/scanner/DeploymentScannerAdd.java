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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operation adding a new {@link DeploymentScannerService}.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    static final DeploymentScannerAdd INSTANCE = new DeploymentScannerAdd();

    private DeploymentScannerAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String path = operation.require(CommonAttributes.PATH).asString();
        final Boolean enabled = operation.hasDefined(CommonAttributes.SCAN_ENABLED) ? operation.get(CommonAttributes.SCAN_ENABLED).asBoolean() : true;
        final Integer interval = operation.hasDefined(CommonAttributes.SCAN_INTERVAL) ? operation.get(CommonAttributes.SCAN_INTERVAL).asInt() : 5000;
        final String relativeTo = operation.hasDefined(CommonAttributes.RELATIVE_TO) ? operation.get(CommonAttributes.RELATIVE_TO).asString() : null;
        final Boolean autoDeployZip = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED) ? operation.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).asBoolean() : true;
        final Boolean autoDeployExp = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED) ? operation.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).asBoolean() : false;
        final Boolean autoDeployXml = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_XML) ? operation.get(CommonAttributes.AUTO_DEPLOY_XML).asBoolean() : true;
        final Long deploymentTimeout = operation.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT) ? operation.get(CommonAttributes.DEPLOYMENT_TIMEOUT).asLong() : 60L;

        model.get(CommonAttributes.NAME).set(name);
        model.get(CommonAttributes.PATH).set(path);
        if (enabled != null) model.get(CommonAttributes.SCAN_ENABLED).set(enabled);
        if (interval != null) model.get(CommonAttributes.SCAN_INTERVAL).set(interval);
        if (autoDeployZip != null) model.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).set(autoDeployZip);
        if (autoDeployExp != null) model.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).set(autoDeployExp);
        if (autoDeployXml != null) model.get(CommonAttributes.AUTO_DEPLOY_XML).set(autoDeployXml);
        if (relativeTo != null) model.get(CommonAttributes.RELATIVE_TO).set(relativeTo);
        if (deploymentTimeout != null) model.get(CommonAttributes.DEPLOYMENT_TIMEOUT).set(deploymentTimeout);

    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String path = operation.require(CommonAttributes.PATH).asString();
        final Boolean enabled = operation.hasDefined(CommonAttributes.SCAN_ENABLED) ? operation.get(CommonAttributes.SCAN_ENABLED).asBoolean() : true;
        final Integer interval = operation.hasDefined(CommonAttributes.SCAN_INTERVAL) ? operation.get(CommonAttributes.SCAN_INTERVAL).asInt() : 5000;
        final String relativeTo = operation.hasDefined(CommonAttributes.RELATIVE_TO) ? operation.get(CommonAttributes.RELATIVE_TO).asString() : null;
        final Boolean autoDeployZip = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED) ? operation.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).asBoolean() : true;
        final Boolean autoDeployExp = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED) ? operation.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).asBoolean() : false;
        final Boolean autoDeployXml = operation.hasDefined(CommonAttributes.AUTO_DEPLOY_XML) ? operation.get(CommonAttributes.AUTO_DEPLOY_XML).asBoolean() : true;
        final Long deploymentTimeout = operation.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT) ? operation.get(CommonAttributes.DEPLOYMENT_TIMEOUT).asLong() : 60L;

        final ServiceTarget serviceTarget = context.getServiceTarget();
        DeploymentScannerService.addService(serviceTarget, name, relativeTo, path, interval, TimeUnit.MILLISECONDS,
                autoDeployZip, autoDeployExp, autoDeployXml, enabled, deploymentTimeout, newControllers, verificationHandler);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentSubsystemDescriptions.getScannerAdd(locale);
    }
}
