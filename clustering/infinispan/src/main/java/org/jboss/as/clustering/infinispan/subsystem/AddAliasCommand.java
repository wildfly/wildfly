package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Custom command to add an alias to a cache-container.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class AddAliasCommand implements OperationStepHandler, DescriptionProvider {

    private static final Logger log = Logger.getLogger(AddAliasCommand.class.getPackage().getName());
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
        final ModelNode currentValue = submodel.get(CommonAttributes.ALIAS.getName()).clone();

        ModelNode newValue = addNewAliasToList(currentValue, newAlias) ;

        // now set the new ALIAS attribute
        final ModelNode syntheticOp = new ModelNode();
        syntheticOp.get(CommonAttributes.ALIAS.getName()).set(newValue);
        CommonAttributes.ALIAS.validateAndSet(syntheticOp, submodel);

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
        return context.getType() == OperationContext.Type.SERVER && !context.isBooting();
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

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getAddAliasCommandDescription(locale);
    }

}
