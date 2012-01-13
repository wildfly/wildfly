/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.server.deployment.AbstractDeploymentHandler.getContents;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Handles deployment into the runtime.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentDeployHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = DEPLOY;

    static ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    public static final DeploymentDeployHandler INSTANCE = new DeploymentDeployHandler();

    private DeploymentDeployHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getDeployDeploymentOperation(locale);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode model = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        model.get(ENABLED).set(true);

        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        final String runtimeName = model.require(RUNTIME_NAME).asString();
        final DeploymentHandlerUtil.ContentItem[] contents = getContents(model.require(CONTENT));
        DeploymentHandlerUtil.deploy(context, runtimeName, name, contents);

        context.completeStep();
    }
}
