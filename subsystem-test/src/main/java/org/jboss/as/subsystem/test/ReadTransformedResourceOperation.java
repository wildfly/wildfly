/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class ReadTransformedResourceOperation implements OperationStepHandler {
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_TRANSFORMED_RESOURCE_OPERATION, null)
            .setPrivateEntry()
            .build();
    private final TransformerRegistry transformerRegistry;
    private final ModelVersion coreModelVersion;
    private final ModelVersion subsystemModelVersion;

    ReadTransformedResourceOperation(final TransformerRegistry transformerRegistry, ModelVersion coreModelVersion, ModelVersion subsystemModelVersion) {
        this.transformerRegistry = transformerRegistry;
        this.coreModelVersion = coreModelVersion;
        this.subsystemModelVersion = subsystemModelVersion;
    }

    private ModelNode transformReadResourceResult(final OperationContext context, ModelNode original, String subsystem) throws OperationFailedException {
        ModelNode rootData = original.get(ModelDescriptionConstants.RESULT);

        Map<PathAddress, ModelVersion> subsystemVersions = new HashMap<PathAddress, ModelVersion>();
        subsystemVersions.put(PathAddress.EMPTY_ADDRESS.append(ModelDescriptionConstants.SUBSYSTEM, subsystem), subsystemModelVersion);

        final TransformationTarget target = TransformationTargetImpl.create(transformerRegistry, coreModelVersion, subsystemVersions, null, TransformationTarget.TransformationTargetType.SERVER);
        final Transformers transformers = Transformers.Factory.create(target);
        final ResourceTransformationContext ctx = Transformers.Factory.getTransformationContext(target, context);

        final ImmutableManagementResourceRegistration rr = context.getRootResourceRegistration();
        Resource root = TransformerRegistry.modelToResource(rr, rootData, true);
        Resource transformed = transformers.transformResource(ctx, root);

        return Resource.Tools.readModel(transformed);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String subsystem = operation.get(ModelDescriptionConstants.SUBSYSTEM).asString();
        // Add a step to transform the result of a READ_RESOURCE.
        // Do this first, Stage.IMMEDIATE
        final ModelNode readResourceResult = new ModelNode();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode transformed = transformReadResourceResult(context, readResourceResult, subsystem);
                context.getResult().set(transformed);
                context.stepCompleted();
            }
        }, OperationContext.Stage.IMMEDIATE);

        // Now add a step to do the READ_RESOURCE, also IMMEDIATE. This will execute *before* the one ^^^
        final ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        context.addStep(readResourceResult, op, ReadResourceHandler.INSTANCE, OperationContext.Stage.IMMEDIATE);
        context.stepCompleted();
    }
}
