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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.RUNTIME_NAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
/**
 * Handles undeployment from the runtime.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUndeployHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = UNDEPLOY;

    private final AbstractVaultReader vaultReader;

    public DeploymentUndeployHandler(final AbstractVaultReader vaultReader) {
        this.vaultReader = vaultReader;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final String deploymentUnitName = RUNTIME_NAME.resolveModelAttribute(context, model).asString();
        model.get(ENABLED.getName()).set(false);

        DeploymentHandlerUtil.undeploy(context, deploymentUnitName, vaultReader);

        context.stepCompleted();
    }
}
