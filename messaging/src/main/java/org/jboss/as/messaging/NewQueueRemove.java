/**
 *
 */
package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Removes a queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class NewQueueRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static NewQueueRemove INSTANCE = new NewQueueRemove();

    private NewQueueRemove() {
    }

    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        ModelNode compensatingOp = NewQueueAdd.getOperation(opAddr, context.getSubModel());
        PathAddress address = PathAddress.pathAddress(opAddr);
        String name = address.getLastElement().getValue();
        if (context instanceof NewRuntimeOperationContext) {
            NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> service = updateContext.getServiceRegistry().getService(MessagingSubsystemElement.CORE_QUEUE_BASE.append(name));
            if(service == null) {
                resultHandler.handleResultComplete(compensatingOp);
            } else {
                service.addListener(new ResultHandler.ServiceRemoveListener(resultHandler, compensatingOp));
            }
        }
        else {
            resultHandler.handleResultComplete(compensatingOp);
        }

        return Cancellable.NULL;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueRemove(locale);
    }

}
