/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/distributed-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
public class DistributedCacheResource extends SharedCacheResource {

    public static final PathElement DISTRIBUTED_CACHE_PATH = PathElement.pathElement(ModelKeys.DISTRIBUTED_CACHE);

    // attributes
    static final SimpleAttributeDefinition L1_LIFESPAN =
            new SimpleAttributeDefinitionBuilder(ModelKeys.L1_LIFESPAN, ModelType.LONG, true)
                    .setXmlName(Attribute.L1_LIFESPAN.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(600000))
                    .build();

    static final SimpleAttributeDefinition OWNERS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.OWNERS, ModelType.INT, true)
                    .setXmlName(Attribute.OWNERS.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(2))
                    .setValidator(new IntRangeValidator(1, true, true))
                    .build();

    @SuppressWarnings("deprecation")
    @Deprecated
    static final SimpleAttributeDefinition VIRTUAL_NODES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.VIRTUAL_NODES, ModelType.INT, true)
                    .setXmlName(Attribute.VIRTUAL_NODES.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1))
                    .setDeprecated(ModelVersion.create(1, 4, 0))
                    .setAlternatives(ModelKeys.SEGMENTS)
                    .build();

    static final SimpleAttributeDefinition SEGMENTS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SEGMENTS, ModelType.INT, true)
                    .setXmlName(Attribute.SEGMENTS.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(80)) // Recommended value is 10 * max_cluster_size.
                    .setValidator(new IntRangeValidator(1, true, true))
                    .setAlternatives(ModelKeys.VIRTUAL_NODES)
                    .build();

    static final AttributeDefinition[] DISTRIBUTED_CACHE_ATTRIBUTES = {OWNERS, SEGMENTS, L1_LIFESPAN};

    public DistributedCacheResource(final ResolvePathHandler resolvePathHandler) {
        super(DISTRIBUTED_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.DISTRIBUTED_CACHE),
                DistributedCacheAdd.INSTANCE,
                CacheRemove.INSTANCE, resolvePathHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(DISTRIBUTED_CACHE_ATTRIBUTES);
        for (AttributeDefinition attr : DISTRIBUTED_CACHE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        // Attribute virtual-nodes has been deprecated, convert to the corresponding segments value if not the default.
        final OperationStepHandler virtualNodesWriteHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                if (operation.hasDefined(VALUE) && operation.get(VALUE).asInt() != 1) {

                    // log a WARN
                    InfinispanLogger.ROOT_LOGGER.virtualNodesAttributeDeprecated();

                    // convert the virtual nodes value to a segments value and write
                    ModelNode convertedValue = SegmentsAndVirtualNodeConverter.virtualNodesToSegments(operation.get(VALUE));
                    final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
                    final ModelNode syntheticOp = new ModelNode();
                    syntheticOp.get(ModelKeys.SEGMENTS).set(convertedValue);
                    SEGMENTS.validateAndSet(syntheticOp, submodel);

                    // since we modified the model, set reload required
                    if (requiresRuntime(context)) {
                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                context.reloadRequired();
                                context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                            }
                        }, OperationContext.Stage.RUNTIME);
                    }
                }
                context.stepCompleted();
            }

            protected boolean requiresRuntime(OperationContext context) {
                return context.getProcessType().isServer() && !context.isBooting();
            }
        };

        // Legacy attributes
        resourceRegistration.registerReadWriteAttribute(VIRTUAL_NODES, null, virtualNodesWriteHandler);
    }
}
