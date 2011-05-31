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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Locale;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Paul Ferraro
 */
public class JGroupsSubsystemDescribe implements NewStepHandler, DescriptionProvider {

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getSubsystemDescribeDescription(locale);
    }

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = context.getResult();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement());
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        result.add(JGroupsSubsystemAdd.createOperation(rootAddress.toModelNode(), subModel));

        if (subModel.hasDefined(ModelKeys.STACK)) {
            for (final Property stack : subModel.get(ModelKeys.STACK).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(ModelKeys.STACK, stack.getName());
                result.add(ProtocolStackAdd.createOperation(address, stack.getValue()));
            }
        }

        context.completeStep();
    }
}
