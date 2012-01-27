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
 * Custom command to add an alias to a cache-container.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class AddAliasCommand implements OperationStepHandler {

    public static final AddAliasCommand INSTANCE = new AddAliasCommand();

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
        final String newAlias = operation.require(NAME).asString();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(CommonAttributes.ALIASES.getName()).clone();

        ModelNode newValue = addNewAliasToList(currentValue, newAlias) ;

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
     * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added. This default implementation
     * returns {@code true} if the {@link OperationContext#getType() context type} is {@link OperationContext.Type#SERVER}
     * and {@link OperationContext#isBooting() context.isBooting()} returns {@code false}.
     *
     * @param context operation context
     * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType().isServer() && !context.isBooting();
    }

    /**
     * Adds new alias to a LIST ModelNode of existing aliases.
     *
     * @param list LIST ModelNode of aliases
     * @param alias
     * @return LIST ModelNode with the added aliases
     */
    private ModelNode addNewAliasToList(ModelNode list, String alias) {

        // check for empty string
        if (alias == null || alias.equals(""))
            return list ;

        // check for undefined list (AS7-3476)
        if (!list.isDefined()) {
            list.setEmptyList();
        }

        ModelNode newList = list.clone() ;
        List<ModelNode> listElements = list.asList();

        boolean found = false;
        for (ModelNode listElement : listElements) {
            if (listElement.asString().equals(alias)) {
                found = true;
            }
        }
        if (!found) {
            newList.add().set(alias);
        }
        return newList ;
    }

}
