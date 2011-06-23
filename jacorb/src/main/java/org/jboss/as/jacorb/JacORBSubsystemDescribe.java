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

package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * <p>
 * This class implements a {@code ModelQueryOperationHandler} that handles the {@code describe} operation in the JacORB
 * subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBSubsystemDescribe implements OperationStepHandler {

    static final JacORBSubsystemDescribe INSTANCE = new JacORBSubsystemDescribe();

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBSubsystemDescribe() {
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) {
        final ModelNode result = new ModelNode();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR))
                .getLastElement());
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

        for (JacORBElement configElement : JacORBElement.getRootElements()) {
            String configElementName = configElement.getLocalName();
            if (subModel.hasDefined(configElementName)) {
                subsystemAdd.get(configElementName).set(subModel.get(configElementName));
            }
        }
        result.add(subsystemAdd);
        context.completeStep();
    }
}
