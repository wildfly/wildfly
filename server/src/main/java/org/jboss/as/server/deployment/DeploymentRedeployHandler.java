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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REDEPLOY;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ALL;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;
import static org.jboss.as.server.deployment.DeploymentHandlerUtil.redeploy;
import static org.jboss.as.server.deployment.DeploymentHandlerUtils.getContents;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;

/**
 * Handles redeployment in the runtime.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRedeployHandler implements OperationStepHandler{

    public static final String OPERATION_NAME = REDEPLOY;

    private final AbstractVaultReader vaultReader;

    public DeploymentRedeployHandler(final AbstractVaultReader vaultReader) {
        this.vaultReader = vaultReader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);
        final String name = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        final String runtimeName = RUNTIME_NAME.resolveModelAttribute(context, model).asString();
        final DeploymentHandlerUtil.ContentItem[] contents = getContents(CONTENT_ALL.resolveModelAttribute(context, model));
        redeploy(context, runtimeName, name, vaultReader, contents);

        context.stepCompleted();
    }
}
