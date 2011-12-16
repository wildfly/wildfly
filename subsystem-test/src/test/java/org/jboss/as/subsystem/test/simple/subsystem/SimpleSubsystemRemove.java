package org.jboss.as.subsystem.test.simple.subsystem;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SimpleSubsystemRemove extends AbstractRemoveStepHandler {

    static final SimpleSubsystemRemove INSTANCE = new SimpleSubsystemRemove();

    private final Logger log = Logger.getLogger(SimpleSubsystemRemove.class);

    private SimpleSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);
        context.removeService(SimpleService.NAME);
    }
}
