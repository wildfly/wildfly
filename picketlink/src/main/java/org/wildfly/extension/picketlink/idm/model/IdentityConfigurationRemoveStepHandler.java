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
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.service.PartitionManagerService;

import java.util.Set;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Silva
 */
public class IdentityConfigurationRemoveStepHandler extends RestartParentResourceRemoveHandler {

    static final IdentityConfigurationRemoveStepHandler INSTANCE = new IdentityConfigurationRemoveStepHandler();

    private IdentityConfigurationRemoveStepHandler() {
        super(ModelElement.PARTITION_MANAGER.getName());
    }

    @Override
    protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress partitionManagerAddress = getParentAddress(context.getCurrentAddress());
        Resource partitionManagerResource = context.readResourceFromRoot(partitionManagerAddress);

        checkIfLastConfiguration(partitionManagerResource);

        ModelNode originalParentModel = Resource.Tools.readModel(partitionManagerResource);

        super.updateModel(context, operation);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                PartitionManagerRemoveHandler.INSTANCE
                    .removeIdentityStoreServices(context, originalParentModel, partitionManagerAddress.getLastElement().getValue(), context.getCurrentAddressValue());

                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
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

    private void checkIfLastConfiguration(Resource partitionManagerResource) throws OperationFailedException {
        Set<Resource.ResourceEntry> configurations = partitionManagerResource.getChildren(ModelElement.IDENTITY_CONFIGURATION.getName());

        if (configurations.isEmpty() || configurations.size() == 1) {
            throw ROOT_LOGGER.idmNoIdentityConfigurationProvided();
        }
    }
}
