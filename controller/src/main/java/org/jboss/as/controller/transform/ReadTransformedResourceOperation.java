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

package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ReadTransformedResourceOperation implements OperationStepHandler {

    private final ParametersValidator validator = new ParametersValidator();

    public static DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    private final TransformerRegistry transformerRegistry;
    private final ModelVersion coreModelVersion;
    private final ModelVersion subsystemModelVersion;

    public ReadTransformedResourceOperation(final TransformerRegistry transformerRegistry, ModelVersion coreModelVersion, ModelVersion subsystemModelVersion) {
        validator.registerValidator(SUBSYSTEM, new ModelTypeValidator(ModelType.STRING, false));
        this.transformerRegistry = transformerRegistry;
        this.coreModelVersion = coreModelVersion;
        this.subsystemModelVersion = subsystemModelVersion;
    }

    private ModelNode transformReadResourceResult(final OperationContext context, ModelNode original, String subsystem) throws OperationFailedException {
        ModelNode rootData = original.get(ModelDescriptionConstants.RESULT);

        Map<PathAddress,ModelVersion> subsystemVersions = new HashMap<PathAddress, ModelVersion>();
        subsystemVersions.put(PathAddress.EMPTY_ADDRESS.append(ModelDescriptionConstants.SUBSYSTEM,subsystem), subsystemModelVersion);

        final TransformationTarget target = TransformationTargetImpl.create(transformerRegistry, coreModelVersion, subsystemVersions , null, TransformationTarget.TransformationTargetType.SERVER);
        final Transformers transformers = Transformers.Factory.create(target);
        final ResourceTransformationContext ctx = Transformers.Factory.getTransformationContext(target, context);

        final ImmutableManagementResourceRegistration rr = context.getRootResourceRegistration();
        Resource root = TransformerRegistry.modelToResource(rr, rootData, true);
        Resource transformed = transformers.transformResource(ctx, root) ;

        final ModelNode model = Resource.Tools.readModel(transformed);

        return model;
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
