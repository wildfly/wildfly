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

package org.jboss.as.jmx;

import static org.jboss.as.jmx.CommonAttributes.USE_MANAGEMENT_ENDPOINT;
import static org.jboss.as.jmx.JMXSubsystemAdd.getDomainName;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Stuart Douglas
 */
class RemotingConnectorAdd extends AbstractAddStepHandler {

    static final RemotingConnectorAdd INSTANCE = new RemotingConnectorAdd();

    private RemotingConnectorAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        if(operation.hasDefined(USE_MANAGEMENT_ENDPOINT)) {
            RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        boolean useManagementEndpoint = true;
        if(model.hasDefined(USE_MANAGEMENT_ENDPOINT)) {
             useManagementEndpoint = RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.resolveModelAttribute(context, model).asBoolean();
        }

        // Read the model for the JMW subsystem to find the domain name for the resolved/expressions models (if they are exposed).
        PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress parentAddress = address.subAddress(0, address.size() - 1);
        ModelNode jmxSubsystemModel = Resource.Tools.readModel(context.readResourceFromRoot(parentAddress, true));
        String resolvedDomain = getDomainName(context, jmxSubsystemModel, CommonAttributes.RESOLVED);
        String expressionsDomain = getDomainName(context, jmxSubsystemModel, CommonAttributes.EXPRESSION);

        launchServices(context, verificationHandler, newControllers, useManagementEndpoint, resolvedDomain, expressionsDomain);
    }

    void launchServices(OperationContext context, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, boolean useManagementEndpoint, String resolvedDomain, String expressionsDomain) {

        final ServiceTarget target = context.getServiceTarget();
        ServiceController<?> controller = RemotingConnectorService.addService(target, verificationHandler, useManagementEndpoint, resolvedDomain, expressionsDomain);
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }
}
