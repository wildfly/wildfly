/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.Set;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Helper class that registers legacy add-metric/remove-metric/add-custom-metric/remove-custom-metric operations.
 * The class in its entirety is designed to be removed (plus one line for registration) once deprecated long enough.
 * Deprecated since model version 6.0.0.
 *
 * @author Radoslav Husar
 */
public class LegacyMetricOperationsRegistration implements Registration<ManagementResourceRegistration> {

    public void register(ManagementResourceRegistration registration) {
        // Transform legacy /subsystem=modcluster/mod-cluster-config=CONFIGURATION:add-metric(type=...) operation
        // -> /subsystem=modcluster/proxy=*/dynamic-load-provider=configuration/load-metric=...:add(...)
        OperationDefinition legacyAddMetricOperation = new SimpleOperationDefinitionBuilder("add-metric", ModClusterExtension.SUBSYSTEM_RESOLVER)
                .addParameter(LoadMetricResourceDefinition.Attribute.TYPE.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.WEIGHT.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.CAPACITY.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.PROPERTY.getDefinition())
                .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                .build();

        OperationStepHandler legacyAddMetricHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                operationDeprecated(context, operation);
                PathAddress address = translateProxyPath(context);
                String type = operation.require(LoadMetricResourceDefinition.Attribute.TYPE.getName()).asString();
                PathAddress metricPath = PathAddress.pathAddress(DynamicLoadProviderResourceDefinition.LEGACY_PATH, LoadMetricResourceDefinition.pathElement(type));
                PathAddress metricPathAddress = address.append(metricPath);
                ModelNode metricOperation = Util.createAddOperation(metricPathAddress);
                OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(metricPath), ModelDescriptionConstants.ADD);
                for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                    String name = attribute.getName();
                    if (operation.hasDefined(name)) {
                        metricOperation.get(name).set(operation.get(name));
                    }
                }
                context.addStep(metricOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL, true);
            }
        };

        registration.registerOperationHandler(legacyAddMetricOperation, legacyAddMetricHandler);

        // Transform legacy /subsystem=modcluster/mod-cluster-config=CONFIGURATION:remove-metric(type=...) operation
        // -> /subsystem=modcluster/proxy=*/dynamic-load-provider=configuration/load-metric=...:remove()
        OperationDefinition legacyRemoveMetricOperation = new SimpleOperationDefinitionBuilder("remove-metric", ModClusterExtension.SUBSYSTEM_RESOLVER)
                .addParameter(LoadMetricResourceDefinition.Attribute.TYPE.getDefinition())
                .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                .build();

        OperationStepHandler legacyRemoveMetricHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                operationDeprecated(context, operation);
                PathAddress address = translateProxyPath(context);
                String type = operation.require(LoadMetricResourceDefinition.Attribute.TYPE.getName()).asString();
                PathAddress metricPath = PathAddress.pathAddress(DynamicLoadProviderResourceDefinition.LEGACY_PATH, LoadMetricResourceDefinition.pathElement(type));
                PathAddress metricPathAddress = address.append(metricPath);
                ModelNode metricOperation = Util.createRemoveOperation(metricPathAddress);
                OperationEntry removeOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(metricPath), ModelDescriptionConstants.REMOVE);
                context.addStep(metricOperation, removeOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL, true);
            }
        };

        registration.registerOperationHandler(legacyRemoveMetricOperation, legacyRemoveMetricHandler);

        // Transform legacy /subsystem=modcluster/mod-cluster-config=CONFIGURATION:add-custom-metric(class=...) operation
        // -> /subsystem=modcluster/proxy=*/dynamic-load-provider=configuration/custom-load-metric=...:add(...)
        OperationDefinition legacyAddCustomMetricOperation = new SimpleOperationDefinitionBuilder("add-custom-metric", ModClusterExtension.SUBSYSTEM_RESOLVER)
                .addParameter(CustomLoadMetricResourceDefinition.Attribute.CLASS.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.WEIGHT.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.CAPACITY.getDefinition())
                .addParameter(LoadMetricResourceDefinition.SharedAttribute.PROPERTY.getDefinition())
                .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                .build();

        OperationStepHandler legacyAddCustomMetricHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                operationDeprecated(context, operation);
                PathAddress address = translateProxyPath(context);
                String type = operation.require(CustomLoadMetricResourceDefinition.Attribute.CLASS.getName()).asString();
                PathAddress metricPath = PathAddress.pathAddress(DynamicLoadProviderResourceDefinition.LEGACY_PATH, LoadMetricResourceDefinition.pathElement(type));
                PathAddress metricPathAddress = address.append(metricPath);
                ModelNode metricOperation = Util.createAddOperation(metricPathAddress);
                OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(metricPath), ModelDescriptionConstants.ADD);
                for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                    String name = attribute.getName();
                    if (operation.hasDefined(name)) {
                        metricOperation.get(name).set(operation.get(name));
                    }
                }
                context.addStep(metricOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL, true);
            }
        };

        registration.registerOperationHandler(legacyAddCustomMetricOperation, legacyAddCustomMetricHandler);

        // Transform legacy /subsystem=modcluster/mod-cluster-config=CONFIGURATION:remove-custom-metric(class=...) operation
        // -> /subsystem=modcluster/proxy=*/dynamic-load-provider=configuration/custom-load-metric=...:remove()
        OperationDefinition legacyRemoveCustomMetricOperation = new SimpleOperationDefinitionBuilder("remove-custom-metric", ModClusterExtension.SUBSYSTEM_RESOLVER)
                .addParameter(CustomLoadMetricResourceDefinition.Attribute.CLASS.getDefinition())
                .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                .build();

        OperationStepHandler legacyRemoveCustomMetricHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                operationDeprecated(context, operation);
                PathAddress address = translateProxyPath(context);
                String type = operation.require(CustomLoadMetricResourceDefinition.Attribute.CLASS.getName()).asString();
                PathAddress metricPath = PathAddress.pathAddress(DynamicLoadProviderResourceDefinition.LEGACY_PATH, LoadMetricResourceDefinition.pathElement(type));
                PathAddress metricPathAddress = address.append(metricPath);
                ModelNode metricOperation = Util.createRemoveOperation(metricPathAddress);
                OperationEntry removeOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(metricPath), ModelDescriptionConstants.REMOVE);
                context.addStep(metricOperation, removeOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL, true);
            }
        };

        registration.registerOperationHandler(legacyRemoveCustomMetricOperation, legacyRemoveCustomMetricHandler);
    }

    static PathAddress translateProxyPath(OperationContext context) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress().getParent();
        Set<Resource.ResourceEntry> children = context.readResourceFromRoot(address).getChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey());
        if (children.size() != 1) {
            throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.legacyOperationsWithMultipleProxies());
        }
        PathAddress proxyPath = PathAddress.pathAddress(ProxyConfigurationResourceDefinition.pathElement(children.iterator().next().getName()));
        address = address.append(proxyPath);
        return address;
    }

    private static void operationDeprecated(OperationContext context, ModelNode operation) {
        ControllerLogger.DEPRECATED_LOGGER.operationDeprecated(Operations.getName(operation), context.getCurrentAddress().toCLIStyleString());
    }

}
