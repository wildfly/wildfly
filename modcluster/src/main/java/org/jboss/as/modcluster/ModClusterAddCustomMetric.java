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
import org.jboss.logging.Logger;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterAddCustomMetric implements OperationStepHandler, DescriptionProvider{

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    static final ModClusterAddCustomMetric INSTANCE = new ModClusterAddCustomMetric();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getAddCustomMetricDescription(locale);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
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
                    if (!metric.get("class").isDefined()) {
                        throw new OperationFailedException(new ModelNode().set("A class attribute is needed for add-custom-metric"));
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
                    if (dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).isDefined()) {
                        String classname = metric.get("class").asString();
                        List<ModelNode> list = dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).asList();
                        Iterator<ModelNode> it = list.iterator();
                        dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).set(newlist);
                        while(it.hasNext()) {
                            ModelNode node = it.next();
                            if (!node.get("class").asString().equals(classname)) {
                                dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).add(node);
                            }
                        }
                    } else {
                        dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).set(newlist);
                    }
                    dynamicLoadProvider.get(CommonAttributes.CUSTOM_LOAD_METRIC).add(metric);
                }
            }, OperationContext.Stage.MODEL);
        }

        context.completeStep();
    }
}
