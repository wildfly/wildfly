/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;

/**
 * Adds a mdb delivery group.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryGroupAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installServices(context, operation, model);
    }

    protected void installServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final boolean active = MdbDeliveryGroupResourceDefinition.ACTIVE.resolveModelAttribute(context, model).asBoolean();

        CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        serviceTarget.addCapability(MdbDeliveryGroupResourceDefinition.MDB_DELIVERY_GROUP_CAPABILITY).setInstance(Service.NULL)
                .setInitialMode(active? ServiceController.Mode.ACTIVE: ServiceController.Mode.NEVER)
                .install();
    }
}
