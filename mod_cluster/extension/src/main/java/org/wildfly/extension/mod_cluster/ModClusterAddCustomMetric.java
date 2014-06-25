/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

public class ModClusterAddCustomMetric implements OperationStepHandler {

    static final ModClusterAddCustomMetric INSTANCE = new ModClusterAddCustomMetric();


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress opAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress parent = opAddress.append(DynamicLoadProviderDefinition.PATH);
        String name = CustomLoadMetricDefinition.CLASS.resolveModelAttribute(context, operation).asString();

        ModelNode targetOperation = Util.createAddOperation(parent.append(PathElement.pathElement(CustomLoadMetricDefinition.PATH.getKey(), name)));

        for (AttributeDefinition def : CustomLoadMetricDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, targetOperation);
        }
        context.addStep(targetOperation, CustomLoadMetricAdd.INSTANCE, OperationContext.Stage.MODEL, true);
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
