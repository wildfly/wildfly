package org.jboss.as.modcluster;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModClusterConfigAdd extends AbstractAddStepHandler {

    static final ModClusterConfigAdd INSTANCE = new ModClusterConfigAdd();

    private ModClusterConfigAdd() {
    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model     persistent configuration model node that corresponds to the address of {@code operation}
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or populating the model otherwise fails
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : ModClusterConfigResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
        ModClusterConfigResourceDefinition.SIMPLE_LOAD_PROVIDER.validateAndSet(operation, model);
    }
}
