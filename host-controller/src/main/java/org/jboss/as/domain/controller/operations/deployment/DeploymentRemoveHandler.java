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
package org.jboss.as.domain.controller.operations.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.dmr.ModelNode;

/**
 * Handles removal of a deployment from the model. This can be used at either the domain deployments level
 * or the server-group deployments level
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final DeploymentRemoveHandler INSTANCE = new DeploymentRemoveHandler();

    private DeploymentRemoveHandler() {
    }

    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String deploymentName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

        final Resource root = context.getRootResource();

        if(root.hasChild(PathElement.pathElement(SERVER_GROUP))) {

            final List<String> badGroups = new ArrayList<String>();
            for(final Resource.ResourceEntry entry : root.getChildren(SERVER_GROUP)) {
                if(entry.hasChild(PathElement.pathElement(DEPLOYMENT, deploymentName))) {
                    badGroups.add(entry.getName());
                }
            }

            if (badGroups.size() > 0) {
                String msg = String.format("Cannot remove deployment %s from the domain as it is still used by server groups %s", deploymentName, badGroups);
                throw new OperationFailedException(new ModelNode().set(msg));
            }
        }
        super.performRemove(context, operation, model);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DomainRootDescription.getDeploymentRemoveOperation(locale);
    }
}
