/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.managedobject;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelReader {
    private final ModelController controller;
    private volatile ModelNode deploymentModel;

    public ModelReader(ModelController controller) {
        this.controller = controller;
    }

    ModelNode getDeploymentModel() {
        if (deploymentModel == null) {
            ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(RECURSIVE).set(true);
            operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT)).toModelNode());
            deploymentModel = execute(operation);
        }
        return deploymentModel;
    }

    ModelNode execute(ModelNode operation) {
        ModelNode result = controller.execute(operation, OperationMessageHandler.logging, OperationTransactionControl.COMMIT, null);
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).toString());
        }
        return result.require(RESULT);
    }

    void setDeploymentModel(ModelNode deploymentModel) {
        this.deploymentModel = deploymentModel;
    }
}
