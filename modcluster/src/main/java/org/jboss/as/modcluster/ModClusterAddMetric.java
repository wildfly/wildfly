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

package org.jboss.as.modcluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

public class ModClusterAddMetric implements OperationStepHandler {

    static final ModClusterAddMetric INSTANCE = new ModClusterAddMetric();


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress parent = PathAddress.pathAddress(ModClusterExtension.SUBSYSTEM_PATH,
                ModClusterExtension.CONFIGURATION_PATH,
                ModClusterExtension.DYNAMIC_LOAD_PROVIDER);
        String name = getNextMetricName(context);
        ModelNode targetOperation = Util.createAddOperation(parent.append(PathElement.pathElement(ModClusterExtension.LOAD_METRIC.getKey(), name)));

        for (AttributeDefinition def : LoadMetricDefinition.ATTRIBUTES) {
            targetOperation.get(def.getName()).set(operation.get(def.getName())); //do not do resolving here as it will be done by target operation
        }
        context.addStep(targetOperation, LoadMetricAdd.INSTANCE, OperationContext.Stage.IMMEDIATE);
        context.completeStep();
    }

    private String getNextMetricName(OperationContext context) {
        //context.
        Resource loadProvider = context.readResource(PathAddress.pathAddress(ModClusterExtension.DYNAMIC_LOAD_PROVIDER));
        int current = loadProvider.getChildrenNames(CommonAttributes.LOAD_METRIC).size();
        return "metric-" + (current + 1);
    }

}
