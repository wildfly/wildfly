package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;

/**
 * Custom command to remove an alias for a cache-container.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class RemoveAliasCommand implements OperationStepHandler {

    public static final RemoveAliasCommand INSTANCE = new RemoveAliasCommand();

    private final ParametersValidator nameValidator = new ParametersValidator();

    /**
     * An attribute write handler which performs special processing for ALIAS attributes.
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws org.jboss.as.controller.OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String aliasToRemove = operation.require(NAME).asString();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(CommonAttributes.ALIASES.getName()).clone();

        ModelNode newValue = removeAliasFromList(currentValue, aliasToRemove) ;

        // now set the new ALIAS attribute
        final ModelNode syntheticOp = new ModelNode();
        syntheticOp.get(CommonAttributes.ALIASES.getName()).set(newValue);
        CommonAttributes.ALIASES.validateAndSet(syntheticOp, submodel);

        // since we modified the model, set reload required
        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.reloadRequired();
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    /**
     * Gets whether a {@link org.jboss.as.controller.OperationContext.Stage#RUNTIME} handler should be added. This default implementation
     * returns {@code true} if the {@link org.jboss.as.controller.OperationContext#getType() context type} is {@link org.jboss.as.controller.OperationContext.Type#SERVER}
     * and {@link org.jboss.as.controller.OperationContext#isBooting() context.isBooting()} returns {@code false}.
     *
     * @param context operation context
     * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.SERVER && !context.isBooting();
    }

    /**
     * Remove an alias from a LIST ModelNode of existing aliases.
     *
     * @param list LIST ModelNode of aliases
     * @param alias
     * @return LIST ModelNode with the alias removed
     */
    private ModelNode removeAliasFromList(ModelNode list, String alias) {

        // check for empty string
        if (alias == null || alias.equals(""))
            return list ;

        ModelNode newList = new ModelNode() ;
        List<ModelNode> listElements = list.asList();

        for (ModelNode listElement : listElements) {
            if (!listElement.asString().equals(alias)) {
                newList.add().set(listElement);
            }
        }
        return newList ;
    }
}
