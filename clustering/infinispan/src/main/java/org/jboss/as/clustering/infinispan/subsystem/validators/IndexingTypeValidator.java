package org.jboss.as.clustering.infinispan.subsystem.validators;

import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.infinispan.subsystem.Indexing;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validator for cache indexing type (NONE, LOCAL, ALL)
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class IndexingTypeValidator extends ModelTypeValidator implements AllowedValuesValidator  {
    private final EnumSet<Indexing> allowedValues;
    private final List<ModelNode> nodeValues;

    public IndexingTypeValidator(final boolean nullable) {
        super(ModelType.STRING, nullable, false);
        allowedValues = EnumSet.allOf(Indexing.class);
        nodeValues = new ArrayList<ModelNode>(allowedValues.size());
        for (Indexing indexing : allowedValues) {
            nodeValues.add(new ModelNode().set(indexing.toString()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {

        super.validateParameter(parameterName, value);
        if (value.isDefined()) {

            // if the value is not defined, we get an illegal argument exception
            Indexing indexing = null ;
            try {
               indexing = Indexing.valueOf(value.asString());
            }
            catch(IllegalArgumentException e) {
                // catch the exception and allow indexing = null
            }

            if (indexing == null || !allowedValues.contains(indexing)) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidParameterValue("indexing", allowedValues.toString())));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }

}

