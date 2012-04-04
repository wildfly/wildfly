package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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


    public ReadTransformedResourceOperation() {
        validator.registerValidator(SUBSYSTEM, new ModelTypeValidator(ModelType.STRING, false));
    }

    private ModelNode transformReadResourceResult(final ImmutableManagementResourceRegistration managementResourceRegistration, ModelNode original, String subsystem, int major, int minor) {
        ModelNode rootData = original.get(ModelDescriptionConstants.RESULT);

        Resource root = TransformerRegistry.modelToResource(managementResourceRegistration, rootData);
        Resource transformed = TransformerRegistry.getInstance().getTransformedSubsystemResource(root, managementResourceRegistration, subsystem, major, minor);

        return Resource.Tools.readModel(transformed);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String subsystem = operation.get(ModelDescriptionConstants.SUBSYSTEM).asString();
        final int major = operation.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).asInt();
        final int minor = operation.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).asInt();
        final ImmutableManagementResourceRegistration rr = context.getResourceRegistration();
        // Add a step to transform the result of a READ_RESOURCE.
        // Do this first, Stage.IMMEDIATE
        final ModelNode readResourceResult = new ModelNode();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode transformed = transformReadResourceResult(rr, readResourceResult, subsystem, major, minor);
                context.getResult().set(transformed);
                context.completeStep();
            }
        }, OperationContext.Stage.IMMEDIATE);

        // Now add a step to do the READ_RESOURCE, also IMMEDIATE. This will execute *before* the one ^^^
        final ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        context.addStep(readResourceResult, op, GlobalOperationHandlers.READ_RESOURCE, OperationContext.Stage.IMMEDIATE);

        context.completeStep();
    }


}
