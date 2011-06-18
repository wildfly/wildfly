/**
 *
 */
package org.jboss.as.server.operations;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Reads the server state.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerStateAttributeHandler implements NewStepHandler {

    private final ControlledProcessState processState;

    public ServerStateAttributeHandler(final ControlledProcessState processState) {
        this.processState = processState;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        context.getResult().set(processState.getState().toString());
        context.completeStep();
    }

}
