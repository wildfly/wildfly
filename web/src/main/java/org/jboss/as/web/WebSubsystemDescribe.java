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

package org.jboss.as.web;

import java.util.Locale;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Emanuel Muckenhuber
 */
class WebSubsystemDescribe implements OperationStepHandler, DescriptionProvider {

    static final WebSubsystemDescribe INSTANCE = new WebSubsystemDescribe();

    /** {@inheritDoc} */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = context.getResult();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        if (subModel.hasDefined(Constants.CONTAINER_CONFIG)) {
            subsystemAdd.get(Constants.CONTAINER_CONFIG).set(subModel.get(Constants.CONTAINER_CONFIG));
        }
        result.add(subsystemAdd);
        if (subModel.hasDefined(Constants.CONNECTOR)) {
            for (final Property connector : subModel.get(Constants.CONNECTOR).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(Constants.CONNECTOR, connector.getName());
                result.add(WebConnectorAdd.getRecreateOperation(address, connector.getValue()));
            }
        }
        if (subModel.hasDefined(Constants.VIRTUAL_SERVER)) {
            for (final Property host : subModel.get(Constants.VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(Constants.VIRTUAL_SERVER, host.getName());
                result.add(WebVirtualHostAdd.getAddOperation(address, host.getValue()));
            }
        }
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode();
    }
}
