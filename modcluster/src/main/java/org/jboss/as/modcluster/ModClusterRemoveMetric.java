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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

public class ModClusterRemoveMetric implements OperationStepHandler {

    static final ModClusterRemoveMetric INSTANCE = new ModClusterRemoveMetric();


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress parent = PathAddress.pathAddress(
                ModClusterExtension.SUBSYSTEM_PATH,
                ModClusterExtension.CONFIGURATION_PATH,
                ModClusterExtension.DYNAMIC_LOAD_PROVIDER);

        String type = LoadMetricDefinition.TYPE.resolveModelAttribute(context, operation).asString();


        String name = getMetricName(context, type);
        if (name == null) {
            context.setRollbackOnly();
            return;
        }
        ModelNode targetOperation = Util.createRemoveOperation(parent.append(PathElement.pathElement(ModClusterExtension.LOAD_METRIC.getKey(), name)));

        context.addStep(targetOperation, new ReloadRequiredRemoveStepHandler(), OperationContext.Stage.IMMEDIATE);
        context.completeStep();
    }

    private String getMetricName(OperationContext context, String type) {
        ModelNode loadProvider = context.readResource(PathAddress.pathAddress(ModClusterExtension.DYNAMIC_LOAD_PROVIDER)).getModel();
        ModelNode loadMetrics = loadProvider.get(CommonAttributes.LOAD_METRIC);
        for (Property p : loadMetrics.asPropertyList()) {
            String metricType = p.getValue().get(CommonAttributes.TYPE).asString();
            if (metricType.equals(type)) {
                return p.getName();
            }
        }
        return null;
    }

}
