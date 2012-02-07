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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.ALL_ATTRIBUTES;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_XML;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_ZIPPED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.DEPLOYMENT_TIMEOUT;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.RELATIVE_TO;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.SCAN_ENABLED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.SCAN_INTERVAL;

/**
 * Operation adding a new {@link DeploymentScannerService}.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
class DeploymentScannerAdd extends AbstractAddStepHandler {
    static final DeploymentScannerAdd INSTANCE = new DeploymentScannerAdd();

    private DeploymentScannerAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition atr : ALL_ATTRIBUTES) {
            atr.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String path = DeploymentScannerDefinition.PATH.resolveModelAttribute(context, operation).asString();
        final Boolean enabled = SCAN_ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final Integer interval = SCAN_INTERVAL.resolveModelAttribute(context, operation).asInt();
        final String relativeTo = operation.hasDefined(CommonAttributes.RELATIVE_TO) ? RELATIVE_TO.resolveModelAttribute(context, operation).asString() : null;
        final Boolean autoDeployZip = AUTO_DEPLOY_ZIPPED.resolveModelAttribute(context, operation).asBoolean();
        final Boolean autoDeployExp = AUTO_DEPLOY_EXPLODED.resolveModelAttribute(context, operation).asBoolean();
        final Boolean autoDeployXml = AUTO_DEPLOY_XML.resolveModelAttribute(context, operation).asBoolean();
        final Long deploymentTimeout = DEPLOYMENT_TIMEOUT.resolveModelAttribute(context, operation).asLong();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        DeploymentScannerService.addService(serviceTarget, name, relativeTo, path, interval, TimeUnit.MILLISECONDS,
                autoDeployZip, autoDeployExp, autoDeployXml, enabled, deploymentTimeout, newControllers, verificationHandler);
    }


}
