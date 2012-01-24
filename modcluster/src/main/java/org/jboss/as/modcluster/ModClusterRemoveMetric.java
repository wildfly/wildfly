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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterRemoveMetric implements OperationStepHandler, DescriptionProvider{

    static final ModClusterRemoveMetric INSTANCE = new ModClusterRemoveMetric();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getRemoveMetricDescription(locale);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        // TODO AS7-3194 no reason this can't run on the Host Controller; it just updates the model
        // TODO AS7-3194 this does not update the runtime! The server needs to be marked reload-required
        if (context.getProcessType().isServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    // Look for the dynamic-load-provider
                    final ModelNode dynamicLoadProvider = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(CommonAttributes.DYNAMIC_LOAD_PROVIDER);
                    String type = null;
                     if (dynamicLoadProvider.isDefined()) {
                         List<Property> list = operation.asPropertyList();
                         Iterator<Property> it = list.iterator();
                         while(it.hasNext()) {
                             Property prop = it.next();
                             if (prop.getName().equals("type")) {
                                 type = prop.getValue().asString();
                                 break;
                             }
                         }
                         if (type != null) {
                             removeMetric( dynamicLoadProvider, type);
                         }
                         if (!dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).isDefined() && !dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).isDefined()) {
                             context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().remove(CommonAttributes.DYNAMIC_LOAD_PROVIDER);
                         }

                     }

                    context.completeStep();
                }

                private void removeMetric(ModelNode dynamicLoadProvider, String type) {
                    List<ModelNode> list = dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).asList();
                    List<ModelNode> newlist = Collections.<ModelNode>emptyList();
                    dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).set(newlist);
                    Iterator<ModelNode> it = list.iterator();
                    while(it.hasNext()) {
                        ModelNode node = it.next();
                        if (!node.get("type").asString().equals(type)) {
                            dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).add(node);
                        }
                    }
                    list = dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).asList();
                    if (list.isEmpty()) {
                        dynamicLoadProvider.remove(CommonAttributes.LOAD_METRIC);
                    }
                }
            }, OperationContext.Stage.MODEL);
        }

        context.completeStep();
    }
}
