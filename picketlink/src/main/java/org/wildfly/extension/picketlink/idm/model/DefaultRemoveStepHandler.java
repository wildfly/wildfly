/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.service.PartitionManagerService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * <p>This remove handler is used during the removal of all partition-manager child resources. Its purpose is restart the
 * identity store services prior to the child removal, so we can stop all store services properly before restarting the parent.</p>
 *
 * @author Pedro Silva
 */
public class DefaultRemoveStepHandler extends RestartParentResourceRemoveHandler {

    static final DefaultRemoveStepHandler INSTANCE = new DefaultRemoveStepHandler();

    private DefaultRemoveStepHandler() {
        super(ModelElement.PARTITION_MANAGER.getName());
    }

    @Override
    protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
        super.updateModel(context, operation);

        PathAddress partitionManagerAddress = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        Resource partitionManagerResource = context.readResourceFromRoot(partitionManagerAddress);
        ModelNode parentModel = Resource.Tools.readModel(partitionManagerResource);

        PartitionManagerAddHandler.INSTANCE.validateModel(context, partitionManagerAddress.getLastElement().getValue(), parentModel);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        PartitionManagerAddHandler.INSTANCE.createPartitionManagerService(context, parentAddress.getLastElement()
            .getValue(), parentModel, false);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return PartitionManagerService.createServiceName(parentAddress.getLastElement().getValue());
    }

}
