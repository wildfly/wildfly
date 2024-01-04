/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Executes an add operation only if the resource does not yet exist.
 * @author Paul Ferraro
 */
public enum AddIfAbsentStepHandler implements OperationStepHandler {
    INSTANCE;

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource parentResource = context.readResourceFromRoot(address.getParent(), false);

        if (!parentResource.hasChild(address.getLastElement())) {
            context.getResourceRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD).execute(context, operation);
        }
    }
}
