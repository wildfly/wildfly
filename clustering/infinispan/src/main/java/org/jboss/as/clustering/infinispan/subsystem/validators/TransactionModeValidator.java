package org.jboss.as.clustering.infinispan.subsystem.validators;

import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.infinispan.subsystem.TransactionMode;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validator for transaction mode (NONE, NON_XA, ...)
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransactionModeValidator extends ModelTypeValidator implements AllowedValuesValidator  {
    private final EnumSet<TransactionMode> allowedValues;
    private final List<ModelNode> nodeValues;

    public TransactionModeValidator(final boolean nullable) {
        super(ModelType.STRING, nullable, false);
        allowedValues = EnumSet.allOf(TransactionMode.class);
        nodeValues = new ArrayList<ModelNode>(allowedValues.size());
        for (TransactionMode mode : allowedValues) {
            nodeValues.add(new ModelNode().set(mode.toString()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {

        super.validateParameter(parameterName, value);
        if (value.isDefined()) {

            TransactionMode mode = null ;
            try {
               mode = TransactionMode.valueOf(value.asString());
            }
            catch(IllegalArgumentException e) {
                // catch the exception and allow indexing = null
            }

            if (mode == null || !allowedValues.contains(mode)) {
               throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidParameterValue("mode", allowedValues.toString())));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }

}
