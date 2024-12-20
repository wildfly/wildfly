/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import java.util.function.Function;

import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * <p>This remove handler is used during the removal of all partition-manager resources.</p>
 *
 * @author Pedro Silva
 */
public class DefaultRemoveStepHandler extends ModelOnlyRemoveStepHandler {

    private final Function<PathAddress, PathAddress> partitionAddressProvider;

    DefaultRemoveStepHandler(final Function<PathAddress, PathAddress> partitionAddressProvider) {
        this.partitionAddressProvider = partitionAddressProvider;
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        context.addStep(((context1, operation1) -> PartitionManagerResourceDefinition.validateModel(context1, partitionAddressProvider.apply(context1.getCurrentAddress()))), OperationContext.Stage.MODEL);
        super.performRemove(context, operation, model);
    }

    @Override
    protected boolean removeChildRecursively(PathElement child) {
        // children only represent configuration details of the parent, and are not independent entities
        return false;
    }

}
