package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;

/**
 * Attribute handler for cache-container resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheReadAttributeHandler implements OperationStepHandler {

    public static final CacheReadAttributeHandler INSTANCE = new CacheReadAttributeHandler();
    private final ParametersValidator nameValidator = new ParametersValidator();

    private CacheReadAttributeHandler() {

    }

    /**
     * A read handler which performs special processing for MODE attributes
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws org.jboss.as.controller.OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();

        final ModelNode submodel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(attributeName).clone();

        context.getResult().set(currentValue);

        // since we are not updating the model, there is no need for a RUNTIME step
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
