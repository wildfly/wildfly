/**
 *
 */
package org.jboss.as.controller.operations.common;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Reads the server state.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ProcessStateAttributeHandler implements OperationStepHandler {

    private final ControlledProcessState processState;

    public ProcessStateAttributeHandler(final ControlledProcessState processState) {
        this.processState = processState;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        context.getResult().set(processState.getState().toString());
        context.completeStep();
    }

}
