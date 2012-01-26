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

package org.jboss.as.host.controller.ignored;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Handles add of an ignored domain resource type.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class IgnoredDomainTypeWriteAttributeHandler implements OperationStepHandler {

    public IgnoredDomainTypeWriteAttributeHandler() {
    }


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String attribute = operation.require(ModelDescriptionConstants.NAME).asString();

        ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
        ModelNode mockOp = new ModelNode();
        mockOp.get(attribute).set(value);

        IgnoreDomainResourceTypeResource resource =
            IgnoreDomainResourceTypeResource.class.cast(context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS));

        if (IgnoredDomainTypeResourceDefinition.NAMES.getName().equals(attribute)) {

            IgnoredDomainTypeResourceDefinition.NAMES.validateOperation(mockOp);

            resource.setNames(value);
        } else if (IgnoredDomainTypeResourceDefinition.WILDCARD.getName().equals(attribute)) {

            IgnoredDomainTypeResourceDefinition.WILDCARD.validateOperation(mockOp);

            resource.setWildcard(IgnoredDomainTypeResourceDefinition.WILDCARD.resolveModelAttribute(context, mockOp).asBoolean());
        }

        boolean booting = context.isBooting();
        if (!booting) {
            context.reloadRequired();
        }

        if (context.completeStep() == OperationContext.ResultAction.KEEP) {
            if (booting) {
                resource.publish();
            }
        } else if (!booting) {
            context.revertReloadRequired();
        }
    }
}
