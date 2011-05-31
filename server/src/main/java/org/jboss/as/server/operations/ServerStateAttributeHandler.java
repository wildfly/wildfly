/**
 *
 */
package org.jboss.as.server.operations;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.ServerOperationContext;
import org.jboss.as.server.ServerOperationHandler;
import org.jboss.dmr.ModelNode;

/**
 * Reads the server state.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerStateAttributeHandler implements NewStepHandler {

    public static final ServerStateAttributeHandler INSTANCE = new ServerStateAttributeHandler();

    private ServerStateAttributeHandler() {
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode result = context.getResult();
        // TODO get server state
        context.completeStep();
    }

}
