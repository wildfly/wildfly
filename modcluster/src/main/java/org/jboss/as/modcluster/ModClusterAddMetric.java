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

import static org.jboss.as.modcluster.ModClusterMessages.MESSAGES;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterAddMetric implements OperationStepHandler, DescriptionProvider{

    static final ModClusterAddMetric INSTANCE = new ModClusterAddMetric();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getAddMetricDescription(locale);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        // TODO AS7-3194 no reason this can't run on the Host Controller; it just updates the model
        // TODO AS7-3194 this does not update the runtime! The server needs to be marked reload-required
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    // Look for the dynamic-load-provider
                    final ModelNode dynamicLoadProvider = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(CommonAttributes.DYNAMIC_LOAD_PROVIDER);

                    // Add the metric to the dynamic-load-provider.
                    final ModelNode metric = new ModelNode();
                    List<Property> list = operation.asPropertyList();
                    Iterator<Property> it = list.iterator();
                    while(it.hasNext()) {
                        Property prop = it.next();
                        if (prop.getName().equals("property")) {
                            String properties = prop.getValue().asString();
                            ModelNode props =  ModelNode.fromString(properties);
                            metric.get("property").set(props);
                        } else {
                            metric.get(prop.getName()).set(prop.getValue().asString());
                        }
                    }
                    if (!metric.get("type").isDefined()) {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.typeAttributeRequired("add-metric")));
                    }
                    if (!dynamicLoadProvider.isDefined()) {
                        // Create a default one.
                        dynamicLoadProvider.get(CommonAttributes.HISTORY).set(9);
                        dynamicLoadProvider.get(CommonAttributes.DECAY).set(2);
                    }
                    replaceMetric(dynamicLoadProvider, metric);

                    context.completeStep();
                }

                private void replaceMetric(ModelNode dynamicLoadProvider, ModelNode metric) {
                    List<ModelNode> newlist = Collections.<ModelNode>emptyList();
                    if (dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).isDefined()) {
                        List<ModelNode> list = dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).asList();
                        String type = metric.get("type").asString();
                        Iterator<ModelNode> it = list.iterator();
                        dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).set(newlist);
                        while(it.hasNext()) {
                            ModelNode node = it.next();
                            if (!node.get("type").asString().equals(type)) {
                                dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).add(node);
                            }
                        }
                    } else {
                        dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).set(newlist);
                    }
                    dynamicLoadProvider.get(CommonAttributes.LOAD_METRIC).add(metric);
                }
            }, OperationContext.Stage.MODEL);
        }

        context.completeStep();
    }
}
