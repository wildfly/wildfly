/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.common;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Builds the legacy AS 7.0 complex interface "criteria" representation from the flattened AS 7.1 representation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 * @deprecated this is a purely temporary class to allow the console to transition to a flattened interface model
 */
@Deprecated
public class InterfaceLegacyCriteriaReadHandler implements OperationStepHandler {

    public static final InterfaceLegacyCriteriaReadHandler INSTANCE = new InterfaceLegacyCriteriaReadHandler();

    private InterfaceLegacyCriteriaReadHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        final ModelNode response = context.getResult();

        boolean wildcard = false;
        for (AttributeDefinition attr : InterfaceDescription.WILDCARD_ATTRIBUTES) {
            if (model.hasDefined(attr.getName())) {
                response.set(attr.getName());
                wildcard = true;
                break;
            }
        }

        if (!wildcard) {
            storeSimpleAttributes(model, response);
            if (model.hasDefined(InterfaceDescription.ANY.getName())) {
                ModelNode added = response.add();
                ModelNode anyList = new ModelNode();
                added.set(InterfaceDescription.ANY.getName(), anyList);
                storeSimpleAttributes(model.get(InterfaceDescription.ANY.getName()), anyList);
            }
            if (model.hasDefined(InterfaceDescription.NOT.getName())) {
                ModelNode added = response.add();
                ModelNode notList = new ModelNode();
                added.set(InterfaceDescription.NOT.getName(), notList);
                storeSimpleAttributes(model.get(InterfaceDescription.NOT.getName()), notList);
            }
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private void storeSimpleAttributes(final ModelNode model, final ModelNode list) {
        for (AttributeDefinition attr : InterfaceDescription.SIMPLE_ATTRIBUTES) {
            if (model.hasDefined(attr.getName())) {
                list.add(attr.getName());
            }
        }

        if (model.hasDefined(InterfaceDescription.INET_ADDRESS.getName())) {
            list.add().set(InterfaceDescription.INET_ADDRESS.getName(), model.get(InterfaceDescription.INET_ADDRESS.getName()));
        }

        if (model.hasDefined(InterfaceDescription.LOOPBACK_ADDRESS.getName())) {
            list.add().set(InterfaceDescription.LOOPBACK_ADDRESS.getName(), model.get(InterfaceDescription.LOOPBACK_ADDRESS.getName()));
        }

        if (model.hasDefined(InterfaceDescription.NIC.getName())) {
            list.add().set(InterfaceDescription.NIC.getName(), model.get(InterfaceDescription.NIC.getName()));
        }

        if (model.hasDefined(InterfaceDescription.NIC_MATCH.getName())) {
            list.add().set(InterfaceDescription.NIC_MATCH.getName(), model.get(InterfaceDescription.NIC_MATCH.getName()));
        }

        if (model.hasDefined(InterfaceDescription.SUBNET_MATCH.getName())) {
            list.add().set(InterfaceDescription.SUBNET_MATCH.getName(), model.get(InterfaceDescription.SUBNET_MATCH.getName()));
        }
    }
}
