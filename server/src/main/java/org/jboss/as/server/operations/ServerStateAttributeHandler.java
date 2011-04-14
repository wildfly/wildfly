/**
 *
 */
package org.jboss.as.server.operations;

import org.jboss.as.controller.BasicOperationResult;
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
public class ServerStateAttributeHandler implements ServerOperationHandler {

    public static final ServerStateAttributeHandler INSTANCE = new ServerStateAttributeHandler();

    private ServerStateAttributeHandler() {
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
            throws OperationFailedException {
        ServerOperationContext serverContext = ServerOperationContext.class.cast(context);

        ModelNode result = new ModelNode().set(serverContext.getController().getState().toString());
        resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, result);
        resultHandler.handleResultComplete();
        return new BasicOperationResult();
    }

}
