/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

    static final MdbDeliveryGroupAdd INSTANCE = new MdbDeliveryGroupAdd();

    private MdbDeliveryGroupAdd() {
        super(MdbDeliveryGroupResourceDefinition.ACTIVE);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installServices(context, operation, model);
    }

    protected void installServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final boolean active = MdbDeliveryGroupResourceDefinition.ACTIVE.resolveModelAttribute(context, model).asBoolean();

        CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        serviceTarget.addCapability(MdbDeliveryGroupResourceDefinition.MDB_DELIVERY_GROUP_CAPABILITY, Service.NULL)
                .setInitialMode(active? ServiceController.Mode.ACTIVE: ServiceController.Mode.NEVER)
                .install();
    }
}
