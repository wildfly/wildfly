package org.jboss.as.clustering.infinispan.subsystem.validators;

import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.infinispan.subsystem.StartMode;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validator for controller start mode (EAGER, LAZY)
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StartModeValidator extends ModelTypeValidator implements AllowedValuesValidator  {
    private final EnumSet<StartMode> allowedValues;
    private final List<ModelNode> nodeValues;

    public StartModeValidator(final boolean nullable) {
        super(ModelType.STRING, nullable, false);
        allowedValues = EnumSet.allOf(StartMode.class);
        nodeValues = new ArrayList<ModelNode>(allowedValues.size());
        for (StartMode mode : allowedValues) {
            nodeValues.add(new ModelNode().set(mode.toString()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {

        super.validateParameter(parameterName, value);
        if (value.isDefined()) {

            StartMode mode = null ;
            try {
               mode = StartMode.valueOf(value.asString());
            }
            catch(IllegalArgumentException e) {
                // catch the exception and allow indexing = null
            }

           if (mode == null || !allowedValues.contains(mode)) {
               throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidParameterValue("start", allowedValues.toString())));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }

}
